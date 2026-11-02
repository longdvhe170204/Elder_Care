package com.eldercare.backend.admission.internal.absence.persistence;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import com.eldercare.backend.admission.internal.absence.domain.Absence;
import com.eldercare.backend.admission.internal.absence.domain.AbsenceType;
import com.eldercare.backend.admission.internal.absence.domain.ExpectedReturnChange;
import com.eldercare.backend.admission.internal.absence.domain.NewAbsence;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.time.BusinessTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Lượt vắng ở bảng gộp {@code absence} (dòng {@code kind = 'ABSENCE'}; data-model mục 0, 8) và lịch sử dự kiến trở lại ở {@code
 * status_history}. Thời điểm lưu theo UTC. Mọi hàm ghi chạy trong transaction của lệnh, dưới khóa theo người (research T-03).
 */
@Repository
public class AbsenceStore {

    /** Loại đối tượng, trùng {@code RetentionProvider.dataType()} để dòng lịch sử xóa cùng chuỗi. */
    public static final String ENTITY_TYPE = "M02.ABSENCE";

    private static final HistoryTrack<Void> EXPECTED_RETURN =
            new HistoryTrack<>(ENTITY_TYPE, "EXPECTED_RETURN", Void.class);

    private static final String SELECT = "SELECT id, resident_id, absence_type, reason, standard_reason, departed_at,"
            + " expected_return_at, returned_at, picked_up_by_id, pickup_record_id, transfer_unit, handover_staff_id,"
            + " takes_medication, manager_notified_at, bed_released_at, previous_absence_id, status, created_by, closed_by"
            + " FROM absence WHERE kind = 'ABSENCE'";

    private final JdbcTemplate jdbc;
    private final StatusHistoryApi history;
    private final BusinessTime time;

    AbsenceStore(JdbcTemplate jdbc, StatusHistoryApi history, BusinessTime time) {
        this.jdbc = jdbc;
        this.history = history;
        this.time = time;
    }

    public long insert(NewAbsence a) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO absence (kind, resident_id, absence_type, reason, standard_reason, departed_at,"
                                    + " expected_return_at, picked_up_by_id, pickup_record_id, transfer_unit,"
                                    + " handover_staff_id, takes_medication, previous_absence_id, status, created_by,"
                                    + " created_at) VALUES ('ABSENCE', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, a.residentId());
                    s.setString(2, a.type().name());
                    s.setObject(3, a.reason());
                    s.setObject(4, a.standardReason());
                    s.setObject(5, utc(a.departedAt()));
                    s.setObject(6, utc(a.expectedReturnAt()));
                    s.setObject(7, a.pickedUpById());
                    s.setObject(8, a.pickupRecordId());
                    s.setObject(9, a.transferUnit());
                    s.setLong(10, a.handoverStaffId());
                    s.setBoolean(11, a.takesMedication());
                    s.setObject(12, a.previousAbsenceId());
                    s.setLong(13, a.createdBy());
                    s.setObject(14, utc(a.createdAt()));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<Absence> find(long id) {
        return jdbc.query(SELECT + " AND id = ?", this::map, id).stream().findFirst();
    }

    /** Lượt vắng đang mở của người cao tuổi (tối đa một). */
    public Optional<Absence> open(long residentId) {
        return jdbc.query(SELECT + " AND resident_id = ? AND status = 'OPEN'", this::map, residentId).stream()
                .findFirst();
    }

    public List<Absence> ofResident(long residentId) {
        return jdbc.query(SELECT + " AND resident_id = ? ORDER BY departed_at DESC, id DESC", this::map, residentId);
    }

    public List<Absence> allOpen() {
        return jdbc.query(SELECT + " AND status = 'OPEN' ORDER BY id", this::map);
    }

    /** Thời điểm rời viện của lượt nối tiếp (chuyển viện khi đang Tạm vắng, FR-057); rỗng nếu không có. */
    public Optional<Instant> successorDepartedAt(long absenceId) {
        return jdbc
                .query(
                        "SELECT departed_at FROM absence WHERE kind = 'ABSENCE' AND previous_absence_id = ?",
                        (rs, i) -> instant(rs, "departed_at"),
                        absenceId)
                .stream()
                .findFirst();
    }

    public void close(long id, Instant returnedAt, long closedBy) {
        jdbc.update(
                "UPDATE absence SET status = 'CLOSED', returned_at = ?, closed_by = ?, locked_at = ? WHERE id = ?",
                utc(returnedAt),
                closedBy,
                utc(time.now()),
                id);
    }

    public void updateExpectedReturn(long id, Instant expectedReturnAt) {
        jdbc.update("UPDATE absence SET expected_return_at = ? WHERE id = ?", utc(expectedReturnAt), id);
    }

    /** Đặt dấu đã báo Quản lý viện một lần (FR-055); {@code false} khi đã báo. */
    public boolean markManagerNotified(long id, Instant at) {
        return jdbc.update(
                        "UPDATE absence SET manager_notified_at = ? WHERE id = ? AND manager_notified_at IS NULL",
                        utc(at),
                        id)
                == 1;
    }

    /** Ghi lệnh Giải phóng giường một lần (FR-055); {@code false} khi đã giải phóng. */
    public boolean markBedReleased(long id, Instant at, long by, String reason) {
        return jdbc.update(
                        "UPDATE absence SET bed_released_at = ?, bed_released_by = ?, bed_release_reason = ? WHERE id = ?"
                                + " AND bed_released_at IS NULL",
                        utc(at),
                        by,
                        reason,
                        id)
                == 1;
    }

    public void addExpectedReturnChange(long absenceId, ExpectedReturnChange change) {
        history.add(
                EXPECTED_RETURN,
                absenceId,
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        change.oldValue() == null ? null : change.oldValue().toString(),
                        change.newValue().toString(),
                        null,
                        change.at(),
                        "EXTEND_EXPECTED_RETURN",
                        change.actorId(),
                        false,
                        new Reason(change.reason()),
                        null,
                        null,
                        null,
                        null,
                        null));
    }

    public List<ExpectedReturnChange> expectedReturnHistory(long absenceId) {
        return history.list(EXPECTED_RETURN, absenceId).stream()
                .map(row -> new ExpectedReturnChange(
                        row.valueBefore() == null ? null : Instant.parse(row.valueBefore()),
                        Instant.parse(row.value()),
                        row.reason(),
                        row.actorId(),
                        row.startsAt()))
                .toList();
    }

    private Absence map(ResultSet rs, int row) throws SQLException {
        return new Absence(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                AbsenceType.valueOf(rs.getString("absence_type")),
                rs.getString("reason"),
                rs.getString("standard_reason"),
                instant(rs, "departed_at"),
                instant(rs, "expected_return_at"),
                instant(rs, "returned_at"),
                (Long) rs.getObject("picked_up_by_id"),
                (Long) rs.getObject("pickup_record_id"),
                rs.getString("transfer_unit"),
                rs.getLong("handover_staff_id"),
                rs.getBoolean("takes_medication"),
                instant(rs, "manager_notified_at"),
                instant(rs, "bed_released_at"),
                (Long) rs.getObject("previous_absence_id"),
                "OPEN".equals(rs.getString("status")),
                rs.getLong("created_by"),
                (Long) rs.getObject("closed_by"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
