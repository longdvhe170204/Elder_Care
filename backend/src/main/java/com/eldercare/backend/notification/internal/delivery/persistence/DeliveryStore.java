package com.eldercare.backend.notification.internal.delivery.persistence;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.notification.internal.delivery.domain.AttemptContext;
import com.eldercare.backend.notification.internal.delivery.domain.Retry;
import com.eldercare.backend.notification.internal.delivery.domain.SendResult;
import com.eldercare.backend.notification.internal.domain.DeliveryAttemptContent;
import com.eldercare.backend.notification.internal.domain.DeliveryResultContent;
import com.eldercare.backend.notification.internal.persistence.M13SharedRows;
import com.eldercare.backend.notification.internal.persistence.M13Sql;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Lần gửi, kết quả lần gửi và thiết bị đẩy (data-model mục 5). */
@Repository
public class DeliveryStore {

    private final JdbcTemplate jdbc;
    private final M13SharedRows shared;

    public DeliveryStore(JdbcTemplate jdbc, M13SharedRows shared) {
        this.jdbc = jdbc;
        this.shared = shared;
    }

    /** Lần gửi chưa có kết quả; rỗng khi không có hoặc đã gửi xong. */
    public Optional<AttemptContext> pending(long attemptId) {
        return jdbc
                .query(
                        "SELECT a.id, a.entity_id, " + M13Sql.channel("a") + ", JSON_VALUE(a.content, '$.address'),"
                                + " n.account_id, n.level, r.title,"
                                + " COALESCE(JSON_VALUE(a.content, '$.content'), r.short_content)"
                                + " FROM record_event a JOIN notification n ON n.id = a.entity_id"
                                + " JOIN notification_request r ON r.id = n.request_id"
                                + " WHERE a.id = ? AND " + M13Sql.attempts("a")
                                + " AND NOT EXISTS (SELECT 1 FROM record_event x WHERE " + M13Sql.resultOf("x", "a")
                                + ")",
                        (rs, i) -> new AttemptContext(
                                rs.getLong(1),
                                rs.getLong(2),
                                rs.getString(3),
                                Optional.ofNullable(rs.getString(4)),
                                Optional.ofNullable(rs.getObject(5, Long.class)),
                                NotificationLevel.valueOf(rs.getString(6)),
                                Optional.ofNullable(rs.getString(7)),
                                Optional.ofNullable(rs.getString(8))),
                        attemptId)
                .stream()
                .findFirst();
    }

    /** Ghi kết quả; {@code false} khi lần gửi đã có kết quả. */
    public boolean insertResult(long attemptId, SendResult result, Instant at) {
        return insertResult(attemptId, result, null, at);
    }

    /** Ghi kết quả kèm lần gửi tin tổng hợp (với {@code BATCHED}, FR-018). */
    public boolean insertResult(long attemptId, SendResult result, Long digestAttemptId, Instant at) {
        try {
            shared.addEvent(
                    DeliveryResultContent.KIND,
                    DeliveryAttemptContent.KIND.code(),
                    attemptId,
                    new DeliveryResultContent(result.outcome(), result.reason().orElse(null), digestAttemptId),
                    null,
                    at,
                    DeliveryResultContent.uniqueKey(attemptId));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Lần gửi kênh ngoài lỗi tạm cần gửi lại (FR-018): lần mới nhất của kênh, số lần thử chưa vượt giới hạn. */
    public List<Retry> retryable(int maxAttemptNo, int limit) {
        return jdbc.query(
                "SELECT a.entity_id, " + M13Sql.channel("a") + ", JSON_VALUE(a.content, '$.address'),"
                        + " JSON_VALUE(a.content, '$.content') FROM record_event r JOIN record_event a"
                        + " ON " + M13Sql.resultOf("r", "a") + " AND " + M13Sql.attempts("a")
                        + " WHERE r.m13_temporary_failure = TRUE"
                        + " AND " + M13Sql.channel("a") + " NOT IN ('IN_APP', 'FAMILY_PORTAL')"
                        + " AND " + M13Sql.attemptNo("a") + " <= ? AND " + M13Sql.latestOfChannel("a")
                        + " ORDER BY a.id LIMIT ?",
                (rs, i) -> new Retry(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                maxAttemptNo,
                limit);
    }

    /**
     * Token FCM còn hiệu lực của một tài khoản. Thiết bị nhận đẩy là dòng {@code simple_record} loại {@code
     * M13.PUSH_DEVICE} của tài khoản, khóa duy nhất theo (tài khoản, thiết bị), trạng thái {@code ACTIVE}, {@code
     * INVALIDATED}; app, token ở {@code content} (gộp bảng {@code push_device} 2026-11-13, research R-15 của 000).
     */
    public List<String> activeTokens(long accountId) {
        return jdbc.queryForList(
                "SELECT content->>'$.fcmToken' FROM simple_record WHERE kind = 'M13.PUSH_DEVICE'"
                        + " AND entity_type = 'M15.ACCOUNT' AND entity_id = ? AND status = 'ACTIVE'",
                String.class,
                accountId);
    }

    /** Đăng ký hoặc cập nhật token FCM của thiết bị đang đăng nhập (research B-05); token mới bỏ dấu vô hiệu. */
    public void registerToken(long accountId, String deviceId, String app, String fcmToken, Instant at) {
        jdbc.update(
                "INSERT INTO simple_record (kind, module, entity_type, entity_id, status, content, unique_key, created_at,"
                        + " created_by) VALUES ('M13.PUSH_DEVICE', 'M13', 'M15.ACCOUNT', ?, 'ACTIVE',"
                        + " JSON_OBJECT('deviceId', ?, 'app', ?, 'fcmToken', ?, 'updatedAt', ?), ?, ?, ?) AS new"
                        + " ON DUPLICATE KEY UPDATE status = 'ACTIVE', content = new.content,"
                        + " version = simple_record.version + 1",
                accountId,
                deviceId,
                app,
                fcmToken,
                at.toString(),
                "M13.PUSH_DEVICE:" + accountId + ":" + deviceId,
                utc(at),
                accountId);
    }

    /** FCM báo token hết hạn: không đẩy tới token này nữa. */
    public void invalidateToken(String token, Instant at) {
        jdbc.update(
                "UPDATE simple_record SET status = 'INVALIDATED', content = JSON_SET(content, '$.invalidatedAt', ?),"
                        + " version = version + 1 WHERE kind = 'M13.PUSH_DEVICE' AND status = 'ACTIVE'"
                        + " AND content->>'$.fcmToken' = ?",
                at.toString(),
                token);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
