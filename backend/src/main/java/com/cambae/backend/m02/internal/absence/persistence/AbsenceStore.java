package com.cambae.backend.m02.internal.absence.persistence;

import com.cambae.backend.m02.internal.absence.domain.Absence;
import com.cambae.backend.m02.internal.absence.domain.AbsenceDay;
import com.cambae.backend.m02.internal.absence.domain.AbsenceType;
import com.cambae.backend.m02.internal.absence.domain.BedHoldChoice;
import com.cambae.backend.m02.internal.absence.domain.BedHoldDecision;
import com.cambae.backend.m02.internal.absence.domain.ExpectedReturnChange;
import com.cambae.backend.m02.internal.absence.domain.ThresholdDay;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Truy cập các bảng {@code absence}, {@code expected_return_history}, {@code bed_hold_decision}, {@code absence_day}
 * (data-model mục 8). Thời điểm lưu theo UTC. Mọi hàm ghi chạy trong transaction của lệnh, dưới khóa {@code
 * resident_residency} của người cao tuổi (research T-03).
 */
@Repository
public class AbsenceStore {

    private static final String COLUMNS = "id, resident_id, absence_type, reason, standard_reason, departed_at,"
            + " expected_return_at, returned_at, picked_up_by_id, pickup_record_id, transfer_unit, handover_staff_id,"
            + " takes_medication, initial_bed_hold, threshold_notified_at, review_date, previous_absence_id, status,"
            + " created_by, closed_by";

    private final JdbcTemplate jdbc;

    AbsenceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Dữ liệu mở một lượt vắng. */
    public record NewAbsence(
            long residentId,
            AbsenceType type,
            String reason,
            String standardReason,
            Instant departedAt,
            Instant expectedReturnAt,
            Long pickedUpById,
            Long pickupRecordId,
            String transferUnit,
            long handoverStaffId,
            boolean takesMedication,
            boolean initialBedHold,
            Long previousAbsenceId,
            long createdBy,
            Instant createdAt) {}

    public long insert(NewAbsence a) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO absence (resident_id, absence_type, reason, standard_reason, departed_at,"
                                    + " expected_return_at, picked_up_by_id, pickup_record_id, transfer_unit,"
                                    + " handover_staff_id, takes_medication, initial_bed_hold, previous_absence_id,"
                                    + " created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
                    s.setBoolean(12, a.initialBedHold());
                    s.setObject(13, a.previousAbsenceId());
                    s.setLong(14, a.createdBy());
                    s.setObject(15, utc(a.createdAt()));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<Absence> find(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM absence WHERE id = ?", this::map, id).stream()
                .findFirst();
    }

    /** Lượt vắng đang mở của người cao tuổi (tối đa một). */
    public Optional<Absence> open(long residentId) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM absence WHERE resident_id = ? AND status = 'OPEN'",
                        this::map,
                        residentId)
                .stream()
                .findFirst();
    }

    public List<Absence> ofResident(long residentId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM absence WHERE resident_id = ? ORDER BY departed_at DESC, id DESC",
                this::map,
                residentId);
    }

    public List<Absence> allOpen() {
        return jdbc.query("SELECT " + COLUMNS + " FROM absence WHERE status = 'OPEN' ORDER BY id", this::map);
    }

    /** Thời điểm rời viện của lượt nối tiếp (chuyển viện khi đang Tạm vắng, FR-057); rỗng nếu không có. */
    public Optional<Instant> successorDepartedAt(long absenceId) {
        return jdbc
                .query(
                        "SELECT departed_at FROM absence WHERE previous_absence_id = ?",
                        (rs, i) -> instant(rs, "departed_at"),
                        absenceId)
                .stream()
                .findFirst();
    }

    public void close(long id, Instant returnedAt, long closedBy) {
        jdbc.update(
                "UPDATE absence SET status = 'CLOSED', returned_at = ?, closed_by = ? WHERE id = ?",
                utc(returnedAt),
                closedBy,
                id);
    }

    public void updateExpectedReturn(long id, Instant expectedReturnAt) {
        jdbc.update("UPDATE absence SET expected_return_at = ? WHERE id = ?", utc(expectedReturnAt), id);
    }

    public void markThresholdNotified(long id, Instant at) {
        jdbc.update("UPDATE absence SET threshold_notified_at = ? WHERE id = ?", utc(at), id);
    }

    public void setReviewDate(long id, LocalDate reviewDate) {
        jdbc.update("UPDATE absence SET review_date = ? WHERE id = ?", reviewDate, id);
    }

    // ---- Lịch sử dự kiến trở lại ------------------------------------------------------------------------------------

    public void addExpectedReturnChange(long absenceId, ExpectedReturnChange change) {
        jdbc.update(
                "INSERT INTO expected_return_history (absence_id, old_value, new_value, reason, actor_id, at)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                absenceId,
                utc(change.oldValue()),
                utc(change.newValue()),
                change.reason(),
                change.actorId(),
                utc(change.at()));
    }

    public List<ExpectedReturnChange> expectedReturnHistory(long absenceId) {
        return jdbc.query(
                "SELECT old_value, new_value, reason, actor_id, at FROM expected_return_history WHERE absence_id = ?"
                        + " ORDER BY id",
                (rs, i) -> new ExpectedReturnChange(
                        instant(rs, "old_value"),
                        instant(rs, "new_value"),
                        rs.getString("reason"),
                        rs.getLong("actor_id"),
                        instant(rs, "at")),
                absenceId);
    }

    // ---- Quyết định giữ giường --------------------------------------------------------------------------------------

    public long addDecision(BedHoldDecision d) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO bed_hold_decision (absence_id, threshold_date, choice, coefficient,"
                                    + " review_date, applies_from, applies_to, reason, decided_by, decided_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, d.absenceId());
                    s.setObject(2, d.thresholdDate());
                    s.setString(3, d.choice().name());
                    s.setBigDecimal(4, d.coefficient());
                    s.setObject(5, d.reviewDate());
                    s.setObject(6, d.appliesFrom());
                    s.setObject(7, d.appliesTo());
                    s.setString(8, d.reason());
                    s.setLong(9, d.decidedBy());
                    s.setObject(10, utc(d.decidedAt()));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    private static final String DECISION_COLUMNS = "SELECT id, absence_id, threshold_date, choice, coefficient,"
            + " review_date, applies_from, applies_to, reason, decided_by, decided_at FROM bed_hold_decision";

    public List<BedHoldDecision> decisions(long absenceId) {
        return jdbc.query(
                DECISION_COLUMNS + " WHERE absence_id = ? ORDER BY applies_from, id", this::mapDecision, absenceId);
    }

    public Optional<BedHoldDecision> decisionById(long id) {
        return jdbc.query(DECISION_COLUMNS + " WHERE id = ?", this::mapDecision, id).stream()
                .findFirst();
    }

    private BedHoldDecision mapDecision(ResultSet rs, int row) throws SQLException {
        return new BedHoldDecision(
                rs.getLong("id"),
                rs.getLong("absence_id"),
                rs.getObject("threshold_date", LocalDate.class),
                BedHoldChoice.valueOf(rs.getString("choice")),
                rs.getBigDecimal("coefficient"),
                rs.getObject("review_date", LocalDate.class),
                rs.getObject("applies_from", LocalDate.class),
                rs.getObject("applies_to", LocalDate.class),
                rs.getString("reason"),
                rs.getLong("decided_by"),
                instant(rs, "decided_at"));
    }

    // ---- Ngày vắng --------------------------------------------------------------------------------------------------

    public List<AbsenceDay> days(long absenceId) {
        return jdbc.query(
                "SELECT `date`, sequence_no, coefficient, hold_bed, hold_policy, policy_line_source, policy_line,"
                        + " pending_decision_flag, no_policy_line_flag, decision_id FROM absence_day"
                        + " WHERE absence_id = ? ORDER BY `date`",
                (rs, i) -> new AbsenceDay(
                        rs.getObject("date", LocalDate.class),
                        rs.getInt("sequence_no"),
                        rs.getBigDecimal("coefficient"),
                        rs.getBoolean("hold_bed"),
                        rs.getString("hold_policy"),
                        rs.getString("policy_line_source"),
                        rs.getString("policy_line"),
                        rs.getBoolean("pending_decision_flag"),
                        rs.getBoolean("no_policy_line_flag"),
                        (Long) rs.getObject("decision_id")),
                absenceId);
    }

    /** Ghi hoặc ghi lại một ngày (khóa chính {@code (absence_id, date)}). */
    public void saveDay(long absenceId, AbsenceDay day, Instant calculatedAt, Instant parameterAt) {
        jdbc.update(
                "INSERT INTO absence_day (absence_id, `date`, sequence_no, coefficient, hold_bed, hold_policy,"
                        + " policy_line_source, policy_line, pending_decision_flag, no_policy_line_flag, decision_id,"
                        + " calculated_at, parameter_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE"
                        + " sequence_no = VALUES(sequence_no), coefficient = VALUES(coefficient),"
                        + " hold_bed = VALUES(hold_bed), hold_policy = VALUES(hold_policy), policy_line_source = VALUES(policy_line_source),"
                        + " policy_line = VALUES(policy_line), pending_decision_flag = VALUES(pending_decision_flag),"
                        + " no_policy_line_flag = VALUES(no_policy_line_flag), decision_id = VALUES(decision_id),"
                        + " calculated_at = VALUES(calculated_at), parameter_at = VALUES(parameter_at)",
                absenceId,
                Date.valueOf(day.date()),
                day.sequenceNo(),
                day.coefficient(),
                day.holdBed(),
                day.holdPolicy(),
                day.policyLineSource(),
                day.policyLine(),
                day.pendingDecision(),
                day.noPolicyLine(),
                day.decisionId(),
                utc(calculatedAt),
                utc(parameterAt));
    }

    /** Ngày vắng đầu tiên đã ghi mà dòng áp dụng có giữ giường khác "Có" (ngưỡng giữ giường, FR-055); một truy vấn. */
    public Optional<ThresholdDay> firstNonHoldDay(long absenceId) {
        return jdbc
                .query(
                        "SELECT `date`, sequence_no, policy_line FROM absence_day WHERE absence_id = ? AND hold_policy <>"
                                + " 'YES' ORDER BY `date` LIMIT 1",
                        (rs, i) -> new ThresholdDay(
                                rs.getObject("date", LocalDate.class),
                                rs.getInt("sequence_no"),
                                rs.getString("policy_line")),
                        absenceId)
                .stream()
                .findFirst();
    }

    /** Xóa các ngày sau {@code lastDay} (đính chính rút ngắn lượt vắng). */
    public void deleteDaysAfter(long absenceId, LocalDate lastDay) {
        jdbc.update("DELETE FROM absence_day WHERE absence_id = ? AND `date` > ?", absenceId, Date.valueOf(lastDay));
    }

    /** Xóa các ngày trước {@code firstDay} (đính chính dời giờ rời viện sang ngày sau). */
    public void deleteDaysBefore(long absenceId, LocalDate firstDay) {
        jdbc.update("DELETE FROM absence_day WHERE absence_id = ? AND `date` < ?", absenceId, Date.valueOf(firstDay));
    }

    // ---- Ánh xạ ----------------------------------------------------------------------------------------------------

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
                rs.getBoolean("initial_bed_hold"),
                instant(rs, "threshold_notified_at"),
                rs.getObject("review_date", LocalDate.class),
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
