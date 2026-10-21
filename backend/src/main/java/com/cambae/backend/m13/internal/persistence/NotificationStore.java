package com.cambae.backend.m13.internal.persistence;

import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.m13.api.dto.AcknowledgementMethod;
import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.internal.domain.DeliveryOutcome;
import com.cambae.backend.m13.internal.domain.NotificationEventType;
import com.cambae.backend.m13.internal.domain.NotificationStatusRules;
import com.cambae.backend.m13.internal.domain.NotificationStatusRules.AttemptFact;
import com.cambae.backend.m13.internal.domain.NotificationStatusRules.EventFact;
import com.cambae.backend.m13.internal.domain.NotificationStatusRules.State;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Thông báo, sự kiện, lần gửi dự kiến và bảng trạng thái dẫn xuất (data-model mục 4, 5; research B-09). Mọi thay đổi
 * trạng thái là một dòng sự kiện hoặc kết quả lần gửi; {@link #recompute} tính lại {@code notification_state} bằng
 * {@link NotificationStatusRules} trong cùng transaction.
 */
@Repository
public class NotificationStore {

    /** Thông báo mới cho một người nhận. */
    public record NewNotification(
            long requestId,
            RecipientType recipientType,
            long recipientId,
            Long accountId,
            List<String> basis,
            Set<InformationType> visibleParts,
            boolean identityOnly,
            NotificationLevel level,
            Instant createdAt) {}

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public NotificationStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Thêm thông báo và dòng trạng thái của nó; trả id. */
    public long insert(NewNotification n) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO notification (request_id, recipient_type, recipient_id, account_id, basis,"
                                    + " visible_parts, identity_only, level, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, n.requestId());
                    ps.setString(2, n.recipientType().name());
                    ps.setLong(3, n.recipientId());
                    ps.setObject(4, n.accountId());
                    ps.setString(5, json.writeValueAsString(n.basis()));
                    ps.setString(6, n.visibleParts().stream().map(Enum::name).collect(Collectors.joining(",")));
                    ps.setBoolean(7, n.identityOnly());
                    ps.setString(8, n.level().name());
                    ps.setObject(9, utc(n.createdAt()));
                    return ps;
                },
                key);
        long id = key.getKey().longValue();
        jdbc.update(
                "INSERT INTO notification_state (notification_id, recipient_key, level, resident_id)"
                        + " SELECT ?, ?, ?, r.resident_id FROM notification_request r WHERE r.id = ?",
                id,
                n.recipientType().name() + ":" + n.recipientId(),
                n.level().name(),
                n.requestId());
        return id;
    }

    /** Ghi sự kiện gửi đi (lúc tiếp nhận, hoặc lúc giải phóng giờ yên tĩnh). */
    public void markSent(long notificationId, Instant at) {
        appendEvent(notificationId, NotificationEventType.SEND, Optional.empty(), Optional.empty(), null, null, at);
    }

    /** Thêm một sự kiện và tính lại trạng thái. */
    public void appendEvent(
            long notificationId,
            NotificationEventType type,
            Optional<AcknowledgementMethod> method,
            Optional<Instant> scheduledSendAt,
            String reason,
            Long actorId,
            Instant at) {
        jdbc.update(
                "INSERT INTO notification_event (notification_id, type, acknowledgement_method, reason,"
                        + " scheduled_send_at, actor_id, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                notificationId,
                type.name(),
                method.map(Enum::name).orElse(null),
                reason,
                scheduledSendAt.map(NotificationStore::utc).orElse(null),
                actorId,
                utc(at));
        recompute(notificationId);
    }

    /** Thêm lần gửi dự kiến (lần thử kế tiếp của kênh); trả id lần gửi. */
    public long planAttempt(long notificationId, String channel, String address, Instant at) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO delivery_attempt (notification_id, channel, address, attempt_no, scheduled_at)"
                                    + " SELECT ?, ?, ?, COALESCE(MAX(attempt_no), 0) + 1, ? FROM delivery_attempt"
                                    + " WHERE notification_id = ? AND channel = ?",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, notificationId);
                    ps.setString(2, channel);
                    ps.setString(3, address);
                    ps.setObject(4, utc(at));
                    ps.setLong(5, notificationId);
                    ps.setString(6, channel);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    /**
     * Tính lại trạng thái từ sự kiện và lần gửi mới nhất của từng kênh. Khóa dòng trạng thái để các kết quả kênh đến
     * đồng thời không ghi đè nhau.
     */
    public void recompute(long notificationId) {
        jdbc.queryForList(
                "SELECT notification_id FROM notification_state WHERE notification_id = ? FOR UPDATE",
                Long.class,
                notificationId);
        List<EventFact> events = jdbc.query(
                "SELECT type, acknowledgement_method, scheduled_send_at, occurred_at FROM notification_event"
                        + " WHERE notification_id = ? ORDER BY occurred_at, id",
                (rs, i) -> new EventFact(
                        NotificationEventType.valueOf(rs.getString(1)),
                        Optional.ofNullable(rs.getString(2)).map(AcknowledgementMethod::valueOf),
                        Optional.ofNullable(rs.getObject(3, LocalDateTime.class))
                                .map(NotificationStore::instant),
                        instant(rs.getObject(4, LocalDateTime.class))),
                notificationId);
        List<AttemptFact> attempts = jdbc.query(
                "SELECT r.result FROM delivery_attempt a LEFT JOIN delivery_result r ON r.attempt_id = a.id"
                        + " WHERE a.notification_id = ? AND a.attempt_no = (SELECT MAX(x.attempt_no) FROM"
                        + " delivery_attempt x WHERE x.notification_id = a.notification_id AND x.channel = a.channel)",
                (rs, i) -> new AttemptFact(Optional.ofNullable(rs.getString(1)).map(DeliveryOutcome::valueOf)),
                notificationId);
        State s = NotificationStatusRules.of(events, attempts);
        jdbc.update(
                "UPDATE notification_state SET status = ?, scheduled_send_at = ?, sent_at = ?, viewed_at = ?,"
                        + " acknowledged_at = ?, acknowledgement_method = ?, hidden = ? WHERE notification_id = ?",
                s.status().map(Enum::name).orElse(null),
                s.scheduledSendAt().map(NotificationStore::utc).orElse(null),
                s.sentAt().map(NotificationStore::utc).orElse(null),
                s.viewedAt().map(NotificationStore::utc).orElse(null),
                s.acknowledgedAt().map(NotificationStore::utc).orElse(null),
                s.acknowledgementMethod().map(Enum::name).orElse(null),
                s.hidden(),
                notificationId);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    private static Instant instant(LocalDateTime at) {
        return at.toInstant(ZoneOffset.UTC);
    }
}
