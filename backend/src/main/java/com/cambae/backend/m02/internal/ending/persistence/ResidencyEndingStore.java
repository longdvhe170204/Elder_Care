package com.cambae.backend.m02.internal.ending.persistence;

import com.cambae.backend.m02.api.dto.EndCase;
import com.cambae.backend.m02.internal.ending.domain.EndingConditionCode;
import com.cambae.backend.m02.internal.ending.domain.EndingExceptionRecord;
import com.cambae.backend.m02.internal.ending.domain.EndingStatus;
import com.cambae.backend.m02.internal.ending.domain.Handover;
import com.cambae.backend.m02.internal.ending.domain.RecipientType;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
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
 * Truy cập các bảng {@code residency_ending}, {@code resident_handover}, {@code ending_condition_exception} (data-model mục
 * 9). Thời điểm lưu theo UTC. Mọi hàm ghi chạy trong transaction của lệnh, dưới khóa {@code resident_residency} của người cao
 * tuổi (research T-03).
 */
@Repository
public class ResidencyEndingStore {

    private static final String COLUMNS = "id, resident_id, end_case, medical_reason, destination, expected_end_date,"
            + " actual_ended_at, requested_by, reason, overdue_since, final_period_ref, status, cancel_reason,"
            + " created_by, created_at";

    private final JdbcTemplate jdbc;

    ResidencyEndingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Dữ liệu lập hồ sơ kết thúc. */
    public record NewEnding(
            long residentId,
            EndCase endCase,
            boolean medicalReason,
            String destination,
            LocalDate expectedEndDate,
            String requestedBy,
            String reason,
            long createdBy,
            Instant createdAt) {}

    public long insert(NewEnding e) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO residency_ending (resident_id, end_case, medical_reason, destination,"
                                    + " expected_end_date, requested_by, reason, created_by, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, e.residentId());
                    s.setString(2, e.endCase().name());
                    s.setBoolean(3, e.medicalReason());
                    s.setObject(4, e.destination());
                    s.setDate(5, Date.valueOf(e.expectedEndDate()));
                    s.setString(6, e.requestedBy());
                    s.setString(7, e.reason());
                    s.setLong(8, e.createdBy());
                    s.setObject(9, utc(e.createdAt()));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<ResidencyEnding> find(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM residency_ending WHERE id = ?", this::map, id).stream()
                .findFirst();
    }

    /** Khóa dòng hồ sơ (sau khi đã khóa {@code resident_residency}). */
    public Optional<ResidencyEnding> lock(long id) {
        return jdbc
                .query("SELECT " + COLUMNS + " FROM residency_ending WHERE id = ? FOR UPDATE", this::map, id)
                .stream()
                .findFirst();
    }

    /** Hồ sơ Đang chuẩn bị của người cao tuổi (tối đa một, FR-062). */
    public Optional<ResidencyEnding> preparingOf(long residentId) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM residency_ending WHERE resident_id = ? AND status = 'PREPARING'",
                        this::map,
                        residentId)
                .stream()
                .findFirst();
    }

    /**
     * Hồ sơ Đang chuẩn bị của người cao tuổi bằng đọc khóa (đọc giá trị mới nhất đã commit): dùng sau khi khóa {@code
     * resident_residency} để kiểm "tối đa một hồ sơ mở" mà không bị ảnh chụp cũ của transaction đánh lừa (FR-062).
     */
    public Optional<ResidencyEnding> lockPreparingOf(long residentId) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM residency_ending WHERE resident_id = ? AND status = 'PREPARING'"
                                + " FOR UPDATE",
                        this::map,
                        residentId)
                .stream()
                .findFirst();
    }

    /** Mọi hồ sơ Đang chuẩn bị mà ngày kết thúc dự kiến đã qua trước {@code day} và chưa mang dấu (FR-066a). */
    public List<ResidencyEnding> preparingBefore(LocalDate day) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM residency_ending WHERE status = 'PREPARING' AND expected_end_date < ?"
                        + " ORDER BY id",
                this::map,
                Date.valueOf(day));
    }

    /** Mọi hồ sơ Đang chuẩn bị đang mang dấu "quá ngày dự kiến". */
    public List<ResidencyEnding> overdue() {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM residency_ending WHERE status = 'PREPARING' AND overdue_since IS NOT NULL"
                        + " ORDER BY id",
                this::map);
    }

    public void setFinalPeriodRef(long id, long ref) {
        jdbc.update("UPDATE residency_ending SET final_period_ref = ?, version = version + 1 WHERE id = ?", ref, id);
    }

    public void updateExpectedEndDate(long id, LocalDate date, LocalDate overdueSince) {
        jdbc.update(
                "UPDATE residency_ending SET expected_end_date = ?, overdue_since = ?, version = version + 1"
                        + " WHERE id = ?",
                Date.valueOf(date),
                overdueSince == null ? null : Date.valueOf(overdueSince),
                id);
    }

    public void setOverdueSince(long id, LocalDate overdueSince) {
        jdbc.update(
                "UPDATE residency_ending SET overdue_since = ?, version = version + 1 WHERE id = ?",
                Date.valueOf(overdueSince),
                id);
    }

    public void complete(long id, Instant actualEndedAt, long closedBy, Instant closedAt) {
        jdbc.update(
                "UPDATE residency_ending SET status = 'COMPLETED', actual_ended_at = ?, closed_by = ?, closed_at = ?,"
                        + " version = version + 1 WHERE id = ?",
                utc(actualEndedAt),
                closedBy,
                utc(closedAt),
                id);
    }

    public void cancel(long id, String reason, long closedBy, Instant closedAt) {
        jdbc.update(
                "UPDATE residency_ending SET status = 'CANCELLED', cancel_reason = ?, closed_by = ?, closed_at = ?,"
                        + " version = version + 1 WHERE id = ?",
                reason,
                closedBy,
                utc(closedAt),
                id);
    }

    // ---- Bàn giao ---------------------------------------------------------------------------------------------------

    public long insertHandover(Handover h) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO resident_handover (ending_id, recipient_type, family_member_id, facility_name,"
                                    + " handover_at, handover_staff_id, recorded_by, recorded_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, h.endingId());
                    s.setString(2, h.recipientType().name());
                    s.setObject(3, h.familyMemberId());
                    s.setObject(4, h.facilityName());
                    s.setObject(5, utc(h.handoverAt()));
                    s.setLong(6, h.handoverStaffId());
                    s.setLong(7, h.recordedBy());
                    s.setObject(8, utc(h.recordedAt()));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<Handover> findHandover(long id) {
        return jdbc
                .query(
                        "SELECT id, ending_id, recipient_type, family_member_id, facility_name, handover_at,"
                                + " handover_staff_id, recorded_by, recorded_at FROM resident_handover WHERE id = ?",
                        this::mapHandover,
                        id)
                .stream()
                .findFirst();
    }

    /** Các lần bàn giao của hồ sơ, bản gốc, cũ nhất trước. */
    public List<Handover> handovers(long endingId) {
        return jdbc.query(
                "SELECT id, ending_id, recipient_type, family_member_id, facility_name, handover_at, handover_staff_id,"
                        + " recorded_by, recorded_at FROM resident_handover WHERE ending_id = ? ORDER BY id",
                this::mapHandover,
                endingId);
    }

    // ---- Ngoại lệ ---------------------------------------------------------------------------------------------------

    public long insertException(Long endingId, long residentId, EndingConditionCode code, long requestId, Instant at) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO ending_condition_exception (ending_id, resident_id, condition_code,"
                                    + " approval_request_id, created_at) VALUES (?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setObject(1, endingId);
                    s.setLong(2, residentId);
                    s.setString(3, code.name());
                    s.setLong(4, requestId);
                    s.setObject(5, utc(at));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Ngoại lệ đã duyệt của hồ sơ, theo thứ tự duyệt. */
    public List<EndingExceptionRecord> exceptionsOf(long endingId) {
        return jdbc.query(
                "SELECT id, ending_id, resident_id, condition_code, approval_request_id, created_at"
                        + " FROM ending_condition_exception WHERE ending_id = ? ORDER BY id",
                this::mapException,
                endingId);
    }

    /** Có ngoại lệ đã duyệt của mục quyết toán sau qua đời, không gắn hồ sơ kết thúc (FR-071a, Q-387). */
    public boolean hasPostDeathSettlementException(long residentId) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM ending_condition_exception WHERE resident_id = ? AND ending_id IS NULL"
                                + " AND condition_code = 'END_H_SETTLEMENT'",
                        Integer.class,
                        residentId)
                > 0;
    }

    // ---- Ánh xạ ----------------------------------------------------------------------------------------------------

    private ResidencyEnding map(ResultSet rs, int row) throws SQLException {
        return new ResidencyEnding(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                EndCase.valueOf(rs.getString("end_case")),
                rs.getBoolean("medical_reason"),
                rs.getString("destination"),
                rs.getObject("expected_end_date", LocalDate.class),
                instant(rs, "actual_ended_at"),
                rs.getString("requested_by"),
                rs.getString("reason"),
                rs.getObject("overdue_since", LocalDate.class),
                (Long) rs.getObject("final_period_ref"),
                EndingStatus.valueOf(rs.getString("status")),
                rs.getString("cancel_reason"),
                rs.getLong("created_by"),
                instant(rs, "created_at"));
    }

    private Handover mapHandover(ResultSet rs, int row) throws SQLException {
        return new Handover(
                rs.getLong("id"),
                rs.getLong("ending_id"),
                RecipientType.valueOf(rs.getString("recipient_type")),
                (Long) rs.getObject("family_member_id"),
                rs.getString("facility_name"),
                instant(rs, "handover_at"),
                rs.getLong("handover_staff_id"),
                rs.getLong("recorded_by"),
                instant(rs, "recorded_at"));
    }

    private EndingExceptionRecord mapException(ResultSet rs, int row) throws SQLException {
        return new EndingExceptionRecord(
                rs.getLong("id"),
                (Long) rs.getObject("ending_id"),
                rs.getLong("resident_id"),
                EndingConditionCode.valueOf(rs.getString("condition_code")),
                rs.getLong("approval_request_id"),
                instant(rs, "created_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
