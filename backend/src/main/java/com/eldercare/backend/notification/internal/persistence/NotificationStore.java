package com.eldercare.backend.notification.internal.persistence;

import com.eldercare.backend.notification.api.dto.AcknowledgementMethod;
import com.eldercare.backend.notification.internal.domain.DeliveryAttemptContent;
import com.eldercare.backend.notification.internal.domain.DeliveryOutcome;
import com.eldercare.backend.notification.internal.domain.NewNotification;
import com.eldercare.backend.notification.internal.domain.NotificationEventContent;
import com.eldercare.backend.notification.internal.domain.NotificationEventType;
import com.eldercare.backend.notification.internal.domain.NotificationStatusRules;
import com.eldercare.backend.notification.internal.domain.NotificationStatusRules.AttemptFact;
import com.eldercare.backend.notification.internal.domain.NotificationStatusRules.EventFact;
import com.eldercare.backend.notification.internal.domain.NotificationStatusRules.State;
import com.eldercare.backend.notification.internal.domain.ResidentFloor;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Thông báo, sự kiện, lần gửi dự kiến và cột trạng thái dẫn xuất (data-model mục 0.1, 4, 5; research B-09). Mọi thay đổi
 * trạng thái là một sự kiện hoặc kết quả lần gửi ở {@code record_event}; {@link #recompute} tính lại cột trạng thái của
 * {@code notification} bằng {@link NotificationStatusRules} trong cùng transaction.
 */
@Repository
public class NotificationStore {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final M13SharedRows shared;

    public NotificationStore(JdbcTemplate jdbc, JsonMapper json, M13SharedRows shared) {
        this.jdbc = jdbc;
        this.json = json;
        this.shared = shared;
    }

    /** Thêm thông báo (người cao tuổi lấy từ yêu cầu); trả id. */
    public long insert(NewNotification n) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO notification (request_id, recipient_type, recipient_id, account_id, basis,"
                                    + " visible_parts, identity_only, level, created_at, q354_case, resident_id)"
                                    + " SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, r.resident_id FROM notification_request r"
                                    + " WHERE r.id = ?",
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
                    ps.setLong(11, n.requestId());
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    /** Khóa dòng thông báo và đọc trạng thái hiện hành (rỗng khi chưa có kết quả quyết định). */
    public Optional<String> lockStatus(long notificationId) {
        return jdbc
                .queryForList("SELECT status FROM notification WHERE id = ? FOR UPDATE", String.class, notificationId)
                .stream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    /** Ghi sự kiện gửi đi (lúc tiếp nhận, hoặc lúc giải phóng giờ yên tĩnh). */
    public void markSent(long notificationId, Instant at) {
        appendEvent(notificationId, NotificationEventType.SEND, Optional.empty(), Optional.empty(), null, null, at);
    }

    /**
     * Thêm một sự kiện và tính lại trạng thái; sự kiện chỉ một lần (gửi, không gửi, xem, xác nhận) đã có thì
     * {@code DuplicateKeyException}.
     */
    public void appendEvent(
            long notificationId,
            NotificationEventType type,
            Optional<AcknowledgementMethod> method,
            Optional<Instant> scheduledSendAt,
            String reason,
            Long actorId,
            Instant at) {
        shared.addEvent(
                NotificationEventContent.KIND,
                NotificationEventContent.NOTIFICATION,
                notificationId,
                new NotificationEventContent(type, method.orElse(null), reason, scheduledSendAt.orElse(null)),
                actorId,
                at,
                NotificationEventContent.uniqueKey(notificationId, type));
        recompute(notificationId);
    }

    /**
     * Lần gửi đầu tiên của thông báo vừa tạo (lần thử số 1); không đọc số lần thử như {@link #planAttempt}, để các lượt
     * tiếp nhận chạy song song không khóa khoảng chỉ mục của nhau.
     */
    public long planFirstAttempt(long notificationId, String channel, String address, Instant at) {
        return planFirstAttempt(notificationId, channel, address, at, null);
    }

    /** Như {@link #planFirstAttempt(long, String, String, Instant)}, dồn vào khung gộp {@code batchWindowId} (FR-016b). */
    public long planFirstAttempt(long notificationId, String channel, String address, Instant at, Long batchWindowId) {
        return attempt(notificationId, new DeliveryAttemptContent(channel, address, 1, batchWindowId, null), at);
    }

    /** Thêm lần gửi dự kiến (lần thử kế tiếp của kênh); trả id lần gửi. */
    public long planAttempt(long notificationId, String channel, String address, Instant at) {
        return planAttempt(notificationId, channel, address, at, null);
    }

    /** Lần thử kế tiếp có nội dung riêng ({@code content}: tin tổng hợp, FR-016b; gửi lại dùng lại nội dung đó). */
    public long planAttempt(long notificationId, String channel, String address, Instant at, String content) {
        int next = jdbc.queryForObject(
                "SELECT COALESCE(MAX(" + M13Sql.attemptNo("a") + "), 0) + 1 FROM record_event a WHERE "
                        + M13Sql.attempts("a") + " AND a.entity_id = ? AND " + M13Sql.channel("a") + " = ?",
                Integer.class,
                notificationId,
                channel);
        return attempt(notificationId, new DeliveryAttemptContent(channel, address, next, null, content), at);
    }

    private long attempt(long notificationId, DeliveryAttemptContent c, Instant at) {
        return shared.addEvent(
                DeliveryAttemptContent.KIND,
                NotificationEventContent.NOTIFICATION,
                notificationId,
                c,
                null,
                at,
                DeliveryAttemptContent.uniqueKey(notificationId, c.channel(), c.attemptNo()));
    }

    /**
     * Tính lại trạng thái từ sự kiện và lần gửi mới nhất của từng kênh. Khóa dòng thông báo để các kết quả kênh đến đồng
     * thời không ghi đè nhau.
     */
    public void recompute(long notificationId) {
        jdbc.queryForList("SELECT id FROM notification WHERE id = ? FOR UPDATE", Long.class, notificationId);
        List<EventFact> events = jdbc.query(
                "SELECT e.content, e.occurred_at FROM record_event e WHERE " + M13Sql.notificationEvents("e")
                        + " AND e.entity_id = ? ORDER BY e.occurred_at, e.id",
                (rs, i) -> {
                    NotificationEventContent c = json.readValue(rs.getString(1), NotificationEventContent.class);
                    return new EventFact(
                            c.type(),
                            Optional.ofNullable(c.acknowledgementMethod()),
                            Optional.ofNullable(c.scheduledSendAt()),
                            instant(rs.getObject(2, LocalDateTime.class)));
                },
                notificationId);
        List<AttemptFact> attempts = jdbc.query(
                "SELECT r.m13_result FROM record_event a LEFT JOIN record_event r ON " + M13Sql.resultOf("r", "a")
                        + " WHERE " + M13Sql.attempts("a") + " AND a.entity_id = ? AND " + M13Sql.latestOfChannel("a"),
                (rs, i) -> new AttemptFact(Optional.ofNullable(rs.getString(1)).map(DeliveryOutcome::valueOf)),
                notificationId);
        State s = NotificationStatusRules.of(events, attempts);
        jdbc.update(
                "UPDATE notification SET status = ?, scheduled_send_at = ?, sent_at = ?, viewed_at = ?,"
                        + " acknowledged_at = ?, acknowledgement_method = ?, hidden = ? WHERE id = ?",
                s.status().map(Enum::name).orElse(null),
                s.scheduledSendAt().map(NotificationStore::utc).orElse(null),
                s.sentAt().map(NotificationStore::utc).orElse(null),
                s.viewedAt().map(NotificationStore::utc).orElse(null),
                s.acknowledgedAt().map(NotificationStore::utc).orElse(null),
                s.acknowledgementMethod().map(Enum::name).orElse(null),
                s.hidden(),
                notificationId);
    }

    /** Người cao tuổi, tầng/khu vực của yêu cầu mà thông báo thuộc về. */
    public Optional<ResidentFloor> scope(long notificationId) {
        return jdbc
                .query(
                        "SELECT r.resident_id, r.floor_area_id FROM notification n JOIN notification_request r"
                                + " ON r.id = n.request_id WHERE n.id = ?",
                        (rs, i) -> new ResidentFloor(rs.getObject(1, Long.class), rs.getObject(2, Long.class)),
                        notificationId)
                .stream()
                .findFirst();
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    private static Instant instant(LocalDateTime at) {
        return at.toInstant(ZoneOffset.UTC);
    }
}
