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
                        + " WHERE r.m13_result = 'FAILED' AND JSON_VALUE(r.content, '$.reason') LIKE 'TEMPORARY%'"
                        + " AND " + M13Sql.channel("a") + " NOT IN ('IN_APP', 'FAMILY_PORTAL')"
                        + " AND " + M13Sql.attemptNo("a") + " <= ? AND " + M13Sql.latestOfChannel("a")
                        + " ORDER BY a.id LIMIT ?",
                (rs, i) -> new Retry(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                maxAttemptNo,
                limit);
    }

    /** Token FCM còn hiệu lực của một tài khoản. */
    public List<String> activeTokens(long accountId) {
        return jdbc.queryForList(
                "SELECT fcm_token FROM push_device WHERE account_id = ? AND invalidated_at IS NULL",
                String.class,
                accountId);
    }

    /** Đăng ký hoặc cập nhật token FCM của thiết bị đang đăng nhập (research B-05); token mới bỏ dấu vô hiệu. */
    public void registerToken(long accountId, String deviceId, String app, String fcmToken, Instant at) {
        jdbc.update(
                "INSERT INTO push_device (account_id, device_id, app, fcm_token, updated_at) VALUES (?, ?, ?, ?, ?)"
                        + " ON DUPLICATE KEY UPDATE app = VALUES(app), fcm_token = VALUES(fcm_token),"
                        + " updated_at = VALUES(updated_at), invalidated_at = NULL",
                accountId,
                deviceId,
                app,
                fcmToken,
                utc(at));
    }

    /** FCM báo token hết hạn: không đẩy tới token này nữa. */
    public void invalidateToken(String token, Instant at) {
        jdbc.update(
                "UPDATE push_device SET invalidated_at = ? WHERE fcm_token = ? AND invalidated_at IS NULL",
                utc(at),
                token);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
