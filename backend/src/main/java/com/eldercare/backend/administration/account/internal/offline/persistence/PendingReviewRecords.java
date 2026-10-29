package com.eldercare.backend.administration.account.internal.offline.persistence;

import com.eldercare.backend.administration.account.internal.offline.domain.PendingReason;
import com.eldercare.backend.administration.account.internal.offline.domain.PendingReviewRecord;
import com.eldercare.backend.administration.account.internal.offline.domain.ReviewStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/** Đọc, ghi bảng {@code pending_review_record} (002 T055; data-model mục 5) bằng JDBC như phần còn lại của M15. */
@Component
public class PendingReviewRecords {

    private static final String COLUMNS = "id, record_type, content, idempotency_key, recorder_id, device_id,"
            + " device_time, synced_at, last_connected_at, pending_reason, floor_area_id, status, decided_by, decided_at,"
            + " rejection_reason, result_reference, version";

    private static final RowMapper<PendingReviewRecord> ROW = (rs, i) -> new PendingReviewRecord(
            rs.getLong("id"),
            rs.getString("record_type"),
            rs.getString("content"),
            rs.getString("idempotency_key"),
            rs.getLong("recorder_id"),
            rs.getString("device_id"),
            instant(rs.getObject("device_time", LocalDateTime.class)),
            instant(rs.getObject("synced_at", LocalDateTime.class)),
            instant(rs.getObject("last_connected_at", LocalDateTime.class)),
            PendingReason.valueOf(rs.getString("pending_reason")),
            rs.getObject("floor_area_id", Long.class),
            ReviewStatus.valueOf(rs.getString("status")),
            rs.getObject("decided_by", Long.class),
            instant(rs.getObject("decided_at", LocalDateTime.class)),
            rs.getString("rejection_reason"),
            rs.getString("result_reference"),
            rs.getLong("version"));

    private final JdbcTemplate jdbc;

    public PendingReviewRecords(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(
            String recordType,
            String content,
            String idempotencyKey,
            long recorderId,
            String deviceId,
            Instant deviceTime,
            Instant syncedAt,
            Instant lastConnectedAt,
            PendingReason reason,
            Long floorAreaId) {
        jdbc.update(
                "INSERT INTO pending_review_record (record_type, content, idempotency_key, recorder_id, device_id,"
                        + " device_time, synced_at, last_connected_at, pending_reason, floor_area_id)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                recordType,
                content,
                idempotencyKey,
                recorderId,
                deviceId,
                utc(deviceTime),
                utc(syncedAt),
                utc(lastConnectedAt),
                reason.name(),
                floorAreaId);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public Optional<PendingReviewRecord> find(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM pending_review_record WHERE id = ?", ROW, id).stream()
                .findFirst();
    }

    /** Khóa dòng cho quyết định: quyết định đầu tiên thắng (000 FR-040). */
    public Optional<PendingReviewRecord> lock(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM pending_review_record WHERE id = ? FOR UPDATE", ROW, id).stream()
                .findFirst();
    }

    /** Bản ghi đang chờ có id lớn hơn {@code afterId}, theo id tăng dần. */
    public List<PendingReviewRecord> pendingAfter(long afterId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM pending_review_record WHERE status = 'PENDING_REVIEW' AND id > ?"
                        + " ORDER BY id",
                ROW,
                afterId);
    }

    public void decide(
            long id, ReviewStatus status, long decidedBy, Instant at, String rejectionReason, String reference) {
        jdbc.update(
                "UPDATE pending_review_record SET status = ?, decided_by = ?, decided_at = ?, rejection_reason = ?,"
                        + " result_reference = ?, version = version + 1 WHERE id = ?",
                status.name(),
                decidedBy,
                utc(at),
                rejectionReason,
                reference,
                id);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
