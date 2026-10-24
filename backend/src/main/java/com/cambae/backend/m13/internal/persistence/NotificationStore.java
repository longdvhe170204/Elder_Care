package com.cambae.backend.m13.internal.persistence;

import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.Q354Case;
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
            Instant createdAt,
            Q354Case q354Case) {}

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
                                    + " visible_parts, identity_only, level, created_at, q354_case)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
                    ps.setString(10, n.q354Case() == null ? null : n.q354Case().name());
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

    /** Khóa dòng trạng thái và đọc trạng thái hiện hành (rỗng khi chưa có kết quả quyết định). */
    public Optional<String> lockStatus(long notificationId) {
        return jdbc
                .queryForList(
                        "SELECT status FROM notification_state WHERE notification_id = ? FOR UPDATE",
                        String.class,
                        notificationId)
                .stream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
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

    /**
     * Lần gửi đầu tiên của thông báo vừa tạo (lần thử số 1); chèn thẳng, không đọc số lần thử như {@link #planAttempt}, để
     * các lượt tiếp nhận chạy song song không khóa khoảng chỉ mục của nhau.
     */
    public long planFirstAttempt(long notificationId, String channel, String address, Instant at) {
        return planFirstAttempt(notificationId, channel, address, at, null);
    }

    /** Như {@link #planFirstAttempt(long, String, String, Instant)}, dồn vào khung gộp {@code batchWindowId} (FR-016b). */
    public long planFirstAttempt(long notificationId, String channel, String address, Instant at, Long batchWindowId) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO delivery_attempt (notification_id, channel, address, attempt_no, scheduled_at,"
                                    + " batch_window_id) VALUES (?, ?, ?, 1, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, notificationId);
                    ps.setString(2, channel);
                    ps.setString(3, address);
                    ps.setObject(4, utc(at));
                    ps.setObject(5, batchWindowId);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    /** Thêm lần gửi dự kiến (lần thử kế tiếp của kênh); trả id lần gửi. */
    public long planAttempt(long notificationId, String channel, String address, Instant at) {
        return planAttempt(notificationId, channel, address, at, null);
    }

    /** Lần thử kế tiếp có nội dung riêng ({@code content}: tin tổng hợp, FR-016b; gửi lại dùng lại nội dung đó). */
    public long planAttempt(long notificationId, String channel, String address, Instant at, String content) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO delivery_attempt (notification_id, channel, address, attempt_no, scheduled_at,"
                                    + " content) SELECT ?, ?, ?, COALESCE(MAX(attempt_no), 0) + 1, ?, ? FROM"
                                    + " delivery_attempt WHERE notification_id = ? AND channel = ?",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, notificationId);
                    ps.setString(2, channel);
                    ps.setString(3, address);
                    ps.setObject(4, utc(at));
                    ps.setString(5, content);
                    ps.setLong(6, notificationId);
                    ps.setString(7, channel);
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
