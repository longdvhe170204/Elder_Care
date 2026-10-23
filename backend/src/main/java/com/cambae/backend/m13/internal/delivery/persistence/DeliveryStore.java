package com.cambae.backend.m13.internal.delivery.persistence;

import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.m13.internal.delivery.domain.AttemptContext;
import com.cambae.backend.m13.internal.delivery.domain.SendResult;
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

    public DeliveryStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lần gửi chưa có kết quả; rỗng khi không có hoặc đã gửi xong. */
    public Optional<AttemptContext> pending(long attemptId) {
        return jdbc
                .query(
                        "SELECT a.id, a.notification_id, a.channel, a.address, n.account_id, n.level, r.title,"
                                + " COALESCE(a.content, r.short_content)"
                                + " FROM delivery_attempt a JOIN notification n ON n.id = a.notification_id"
                                + " JOIN notification_request r ON r.id = n.request_id"
                                + " WHERE a.id = ? AND NOT EXISTS (SELECT 1 FROM delivery_result x WHERE x.attempt_id = a.id)",
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
            jdbc.update(
                    "INSERT INTO delivery_result (attempt_id, result, reason, digest_attempt_id, occurred_at)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    attemptId,
                    result.outcome().name(),
                    result.reason().orElse(null),
                    digestAttemptId,
                    utc(at));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Lần gửi kênh ngoài lỗi tạm cần gửi lại (FR-018): lần mới nhất của kênh, số lần thử chưa vượt giới hạn. */
    public record Retry(long notificationId, String channel, String address, String content) {}

    public List<Retry> retryable(int maxAttemptNo, int limit) {
        return jdbc.query(
                "SELECT a.notification_id, a.channel, a.address, a.content FROM delivery_attempt a"
                        + " JOIN delivery_result r ON r.attempt_id = a.id"
                        + " WHERE a.channel <> 'IN_APP' AND a.channel <> 'FAMILY_PORTAL' AND r.result = 'FAILED'"
                        + " AND r.reason LIKE 'TEMPORARY%' AND a.attempt_no <= ?"
                        + " AND a.attempt_no = (SELECT MAX(x.attempt_no) FROM delivery_attempt x"
                        + " WHERE x.notification_id = a.notification_id AND x.channel = a.channel)"
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
