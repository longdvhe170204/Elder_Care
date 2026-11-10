package com.eldercare.backend.admission.internal.ending.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.internal.ending.domain.EndingConditionCode;
import com.eldercare.backend.admission.internal.ending.domain.EndingExceptionRecord;
import com.eldercare.backend.admission.internal.ending.domain.EndingStatus;
import com.eldercare.backend.admission.internal.ending.domain.ExceptionContent;
import com.eldercare.backend.admission.internal.ending.domain.Handover;
import com.eldercare.backend.admission.internal.ending.domain.HandoverContent;
import com.eldercare.backend.admission.internal.ending.domain.NewEnding;
import com.eldercare.backend.admission.internal.ending.domain.ResidencyEnding;
import com.eldercare.backend.common.error.NotFoundException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Hồ sơ kết thúc ở dòng {@code ENDING} của bảng gộp {@code residency_ending}; bàn giao, ngoại lệ điều kiện ở {@code record_event}
 * (data-model mục 0, 9; R-15). Thời điểm lưu theo UTC. Mọi hàm ghi chạy trong transaction của lệnh, dưới khóa theo người (research
 * T-03). Hồ sơ Hoàn tất hoặc Đã hủy khóa dòng (FR-067).
 */
@Repository
public class ResidencyEndingStore {

    /** Loại đối tượng cha của bàn giao, trùng {@code RetentionProvider.dataType()} của hồ sơ kết thúc. */
    private static final String ENDING_ENTITY = "M02.RESIDENCY_ENDING";
    /** Ngoại lệ gắn theo người vì có thể không có hồ sơ kết thúc (FR-071a, Q-387). */
    private static final String RESIDENT_ENTITY = "M01.RESIDENT";

    private static final String SELECT =
            "SELECT id, resident_id, end_case, medical_reason, destination, expected_end_date,"
                    + " actual_ended_at, requested_by, reason, overdue_since, final_period_ref, status, cancel_reason,"
                    + " created_by, created_at FROM residency_ending WHERE kind = 'ENDING'";

    private final JdbcTemplate jdbc;
    private final RecordEventApi events;

    ResidencyEndingStore(JdbcTemplate jdbc, RecordEventApi events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    public long insert(NewEnding e) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO residency_ending (kind, status, resident_id, end_case, medical_reason, destination,"
                                    + " expected_end_date, requested_by, reason, created_by, created_at)"
                                    + " VALUES ('ENDING', 'PREPARING', ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
        return jdbc.query(SELECT + " AND id = ?", this::map, id).stream().findFirst();
    }

    /** Hồ sơ theo người, trạng thái; mã giảm dần. Tham số {@code null} hoặc rỗng là không lọc. */
    public List<ResidencyEnding> list(Long residentId, Set<EndingStatus> statuses) {
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> args = new ArrayList<>();
        if (residentId != null) {
            sql.append(" AND resident_id = ?");
            args.add(residentId);
        }
        if (statuses != null && !statuses.isEmpty()) {
            sql.append(" AND status IN (")
                    .append(statuses.stream().map(s -> "?").collect(Collectors.joining(", ")))
                    .append(")");
            statuses.forEach(s -> args.add(s.name()));
        }
        sql.append(" ORDER BY id DESC");
        return jdbc.query(sql.toString(), this::map, args.toArray());
    }

    /** Khóa dòng hồ sơ (sau khi đã khóa theo người). */
    public Optional<ResidencyEnding> lock(long id) {
        return jdbc.query(SELECT + " AND id = ? FOR UPDATE", this::map, id).stream()
                .findFirst();
    }

    /** Hồ sơ Đang chuẩn bị của người cao tuổi (tối đa một, FR-062). */
    public Optional<ResidencyEnding> preparingOf(long residentId) {
        return jdbc.query(SELECT + " AND resident_id = ? AND status = 'PREPARING'", this::map, residentId).stream()
                .findFirst();
    }

    /**
     * Hồ sơ Đang chuẩn bị của người cao tuổi bằng đọc khóa (đọc giá trị mới nhất đã commit): dùng sau khi khóa theo người để kiểm "tối đa một hồ sơ mở" mà không bị ảnh chụp cũ của transaction đánh lừa (FR-062).
     */
    public Optional<ResidencyEnding> lockPreparingOf(long residentId) {
        return jdbc
                .query(SELECT + " AND resident_id = ? AND status = 'PREPARING' FOR UPDATE", this::map, residentId)
                .stream()
                .findFirst();
    }

    /** Mọi hồ sơ Đang chuẩn bị mà ngày kết thúc dự kiến đã qua trước {@code day} và chưa mang dấu (FR-066a). */
    public List<ResidencyEnding> preparingBefore(LocalDate day) {
        return jdbc.query(
                SELECT + " AND status = 'PREPARING' AND expected_end_date < ? ORDER BY id",
                this::map,
                Date.valueOf(day));
    }

    /** Mọi hồ sơ Đang chuẩn bị đang mang dấu "quá ngày dự kiến". */
    public List<ResidencyEnding> overdue() {
        return jdbc.query(SELECT + " AND status = 'PREPARING' AND overdue_since IS NOT NULL ORDER BY id", this::map);
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

    /** Thời điểm thực tế Kết thúc lưu trú của hồ sơ Hoàn tất mới nhất (mốc dừng tính phí, FR-066). */
    public Optional<Instant> completedAt(long residentId) {
        return jdbc
                .query(
                        "SELECT actual_ended_at FROM residency_ending WHERE kind = 'ENDING' AND resident_id = ? AND status ="
                                + " 'COMPLETED' ORDER BY id DESC LIMIT 1",
                        (rs, i) -> instant(rs, "actual_ended_at"),
                        residentId)
                .stream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    public void complete(long id, Instant actualEndedAt, long closedBy, Instant closedAt) {
        jdbc.update(
                "UPDATE residency_ending SET status = 'COMPLETED', actual_ended_at = ?, closed_by = ?, closed_at = ?,"
                        + " locked_at = ?, version = version + 1 WHERE id = ?",
                utc(actualEndedAt),
                closedBy,
                utc(closedAt),
                utc(closedAt),
                id);
    }

    public void cancel(long id, String reason, long closedBy, Instant closedAt) {
        jdbc.update(
                "UPDATE residency_ending SET status = 'CANCELLED', cancel_reason = ?, closed_by = ?, closed_at = ?,"
                        + " locked_at = ?, version = version + 1 WHERE id = ?",
                reason,
                closedBy,
                utc(closedAt),
                utc(closedAt),
                id);
    }

    // ---- Bàn giao ---------------------------------------------------------------------------------------------------

    public long insertHandover(Handover h, Long pickupRecordId) {
        return events.add(
                        HandoverContent.KIND,
                        new EntityRef(ENDING_ENTITY, h.endingId()),
                        new NewRecordEvent<>(
                                new HandoverContent(
                                        h.recipientType(),
                                        h.familyMemberId(),
                                        h.facilityName(),
                                        h.handoverAt(),
                                        h.handoverStaffId(),
                                        pickupRecordId),
                                null,
                                h.recordedBy(),
                                false,
                                h.recordedAt(),
                                null,
                                h.recordedAt(),
                                null,
                                null))
                .id();
    }

    public Optional<Handover> findHandover(long id) {
        try {
            return Optional.of(handover(events.require(HandoverContent.KIND, id)));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    /** Các lần bàn giao của hồ sơ, bản gốc, cũ nhất trước. */
    public List<Handover> handovers(long endingId) {
        return events.list(HandoverContent.KIND, new EntityRef(ENDING_ENTITY, endingId)).stream()
                .map(ResidencyEndingStore::handover)
                .toList();
    }

    private static Handover handover(RecordEvent<HandoverContent> event) {
        HandoverContent c = event.content();
        return new Handover(
                event.id(),
                event.parent().entityId(),
                c.recipientType(),
                c.familyMemberId(),
                c.facilityName(),
                c.handoverAt(),
                c.handoverStaffId(),
                event.actorId(),
                event.occurredAt());
    }

    // ---- Ngoại lệ ---------------------------------------------------------------------------------------------------

    public long insertException(Long endingId, long residentId, EndingConditionCode code, long requestId, Instant at) {
        return events.add(
                        ExceptionContent.KIND,
                        new EntityRef(RESIDENT_ENTITY, residentId),
                        new NewRecordEvent<>(
                                new ExceptionContent(endingId, code, requestId),
                                null,
                                null,
                                true,
                                at,
                                null,
                                at,
                                null,
                                "M02.ENDING_EXCEPTION:" + requestId))
                .id();
    }

    /** Ngoại lệ đã duyệt của hồ sơ, theo thứ tự duyệt. */
    public List<EndingExceptionRecord> exceptionsOf(long endingId) {
        long residentId = find(endingId).orElseThrow().residentId();
        return exceptionRecords(residentId).stream()
                .filter(e -> Objects.equals(e.endingId(), endingId))
                .toList();
    }

    /** Có ngoại lệ đã duyệt của mục quyết toán sau qua đời, không gắn hồ sơ kết thúc (FR-071a, Q-387). */
    public boolean hasPostDeathSettlementException(long residentId) {
        return exceptionRecords(residentId).stream()
                .anyMatch(e -> e.endingId() == null && e.conditionCode() == EndingConditionCode.END_H_SETTLEMENT);
    }

    private List<EndingExceptionRecord> exceptionRecords(long residentId) {
        return events.list(ExceptionContent.KIND, new EntityRef(RESIDENT_ENTITY, residentId)).stream()
                .map(event -> new EndingExceptionRecord(
                        event.id(),
                        event.content().endingId(),
                        residentId,
                        event.content().conditionCode(),
                        event.content().approvalRequestId(),
                        event.occurredAt()))
                .toList();
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

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
