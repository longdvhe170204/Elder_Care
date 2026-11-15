package com.eldercare.backend.resident.internal.status.persistence;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.administration.shared.api.dto.HistoryRow;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.status.domain.ResidentStatusHistory;
import com.eldercare.backend.resident.internal.status.domain.StatusChangeDetails;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lịch sử trạng thái người cao tuổi ở {@code status_history} (001 T084; thay {@code resident_status_history}): ghi qua
 * {@link StatusHistoryApi} trong transaction của lệnh; đọc theo hồ sơ qua API, đọc một dòng theo mã bằng SQL (API chưa có
 * hàm đọc theo mã), chỉ dòng của {@link ResidentStatusHistory#TRACK}.
 */
@Repository
public class ResidentStatusHistoryStore {

    private final StatusHistoryApi histories;
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    ResidentStatusHistoryStore(StatusHistoryApi histories, JdbcTemplate jdbc, JsonMapper json) {
        this.histories = histories;
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Thêm dòng; lý do thiếu ở lệnh không do hệ thống thì {@code StatusHistoryApi} từ chối (FR-046). */
    public ResidentStatusHistory append(ResidentStatusHistory row) {
        long id = histories.add(
                ResidentStatusHistory.TRACK,
                row.getResidentId(),
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        row.getFromStatus() == null ? null : row.getFromStatus().name(),
                        row.getToStatus().name(),
                        null,
                        row.getOccurredAt(),
                        row.getCommand(),
                        row.getPerformedBy(),
                        row.isBySystem(),
                        row.getReason() == null ? null : new Reason(row.getReason()),
                        row.getBasisType(),
                        row.getBasisId(),
                        new StatusChangeDetails(row.getConditionWaiver()),
                        null,
                        null));
        return row.withId(id);
    }

    /**
     * Ghi thời điểm đúng của dòng {@code target} (014 FR-031a (a)): dòng mới ở {@link ResidentStatusHistory#TIME_TRACK},
     * dòng gốc giữ nguyên (DBR-23).
     */
    public void appendTimeCorrection(
            ResidentStatusHistory target,
            Instant correctedAt,
            long actorId,
            Reason reason,
            String basisType,
            Long basisId) {
        histories.add(
                ResidentStatusHistory.TIME_TRACK,
                target.getResidentId(),
                new HistoryEntry<Void>(
                        HistoryKind.POINT,
                        target.getOccurredAt().toString(),
                        correctedAt.toString(),
                        target.getId(),
                        correctedAt,
                        ResidentStatusHistory.CORRECT_TIME,
                        actorId,
                        false,
                        reason,
                        basisType,
                        basisId,
                        null,
                        null,
                        null));
    }

    /** Thời điểm đã đính chính mới nhất theo mã dòng gốc. */
    private Map<Long, Instant> correctedTimes(long residentId) {
        var result = new HashMap<Long, Instant>();
        histories.list(ResidentStatusHistory.TIME_TRACK, residentId).stream()
                .sorted(Comparator.comparingLong(HistoryRow::id))
                .forEach(r -> result.put(r.valueId(), r.startsAt()));
        return result;
    }

    /** Theo thứ tự ghi; thời điểm là thời điểm đã đính chính nếu có (014 FR-031a (a)). */
    public List<ResidentStatusHistory> findByResidentIdOrderByIdAsc(long residentId) {
        var corrected = correctedTimes(residentId);
        return histories.list(ResidentStatusHistory.TRACK, residentId).stream()
                .sorted(Comparator.comparingLong(HistoryRow::id))
                .map(r -> new ResidentStatusHistory(
                        r.id(),
                        residentId,
                        r.valueBefore() == null ? null : ResidentStatus.valueOf(r.valueBefore()),
                        ResidentStatus.valueOf(r.value()),
                        r.command(),
                        r.startsAt(),
                        r.actorId(),
                        r.bySystem(),
                        r.reason(),
                        r.basisType(),
                        r.basisId(),
                        r.details() == null ? null : r.details().conditionWaiver()))
                .map(h -> corrected.containsKey(h.getId()) ? h.withOccurredAt(corrected.get(h.getId())) : h)
                .toList();
    }

    public Optional<ResidentStatusHistory> findById(long id) {
        return findOriginal(id)
                .map(h -> Optional.ofNullable(correctedTimes(h.getResidentId()).get(id))
                        .map(h::withOccurredAt)
                        .orElse(h));
    }

    private Optional<ResidentStatusHistory> findOriginal(long id) {
        return jdbc
                .query(
                        "SELECT entity_id, value_before, value, command, starts_at, actor_id, by_system, reason,"
                                + " basis_type, basis_id, details FROM status_history WHERE id = ? AND entity_type = ?"
                                + " AND attribute = ?",
                        (rs, i) -> {
                            String details = rs.getString("details");
                            StatusChangeDetails d =
                                    details == null ? null : json.readValue(details, StatusChangeDetails.class);
                            String before = rs.getString("value_before");
                            return new ResidentStatusHistory(
                                    id,
                                    rs.getLong("entity_id"),
                                    before == null ? null : ResidentStatus.valueOf(before),
                                    ResidentStatus.valueOf(rs.getString("value")),
                                    rs.getString("command"),
                                    rs.getObject("starts_at", LocalDateTime.class)
                                            .toInstant(ZoneOffset.UTC),
                                    rs.getLong("actor_id"),
                                    rs.getBoolean("by_system"),
                                    rs.getString("reason"),
                                    rs.getString("basis_type"),
                                    rs.getObject("basis_id", Long.class),
                                    d == null ? null : d.conditionWaiver());
                        },
                        id,
                        ResidentStatusHistory.TRACK.entityType(),
                        ResidentStatusHistory.TRACK.attribute())
                .stream()
                .findFirst();
    }

    public boolean existsById(long id) {
        return findOriginal(id).isPresent();
    }
}
