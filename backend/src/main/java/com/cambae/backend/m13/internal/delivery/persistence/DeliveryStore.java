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
                        "SELECT a.id, a.notification_id, a.channel, a.address, n.account_id, n.level, r.title"
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
                                Optional.ofNullable(rs.getString(7))),
                        attemptId)
                .stream()
                .findFirst();
    }

    /** Ghi kết quả; {@code false} khi lần gửi đã có kết quả. */
    public boolean insertResult(long attemptId, SendResult result, Instant at) {
        try {
            jdbc.update(
                    "INSERT INTO delivery_result (attempt_id, result, reason, occurred_at) VALUES (?, ?, ?, ?)",
                    attemptId,
                    result.outcome().name(),
                    result.reason().orElse(null),
                    utc(at));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Token FCM còn hiệu lực của một tài khoản. */
    public List<String> activeTokens(long accountId) {
        return jdbc.queryForList(
                "SELECT fcm_token FROM push_device WHERE account_id = ? AND invalidated_at IS NULL",
                String.class,
                accountId);
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
