package com.cambae.backend.m13.internal.quiethours.persistence;

import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.m13.api.dto.RecipientType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Thông báo đang Chờ hết giờ yên tĩnh (research B-06): tìm tới hạn, khóa và đọc lại để xét điều kiện lúc gửi. */
@Repository
public class HeldNotificationStore {

    /** Thông báo đang giữ cùng yêu cầu của nó, đủ để xét lại người nhận (FR-025). */
    public record Held(
            long id,
            RecipientType recipientType,
            long recipientId,
            List<String> basis,
            NotificationLevel level,
            Long residentId,
            Optional<InformationSubject> informationSubject,
            boolean hasQ354Case,
            boolean invalidOutOfAppContent,
            Set<InformationType> requestParts,
            Instant scheduledSendAt) {}

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    HeldNotificationStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Id thông báo đang giữ có thời điểm gửi dự kiến tới {@code now}, theo thứ tự tiếp nhận yêu cầu (FR-025). */
    public List<Long> due(Instant now, int limit) {
        return jdbc.queryForList(
                "SELECT s.notification_id FROM notification_state s"
                        + " JOIN notification n ON n.id = s.notification_id"
                        + " JOIN notification_request r ON r.id = n.request_id"
                        + " WHERE s.status = 'HELD_FOR_QUIET_HOURS' AND s.scheduled_send_at <= ?"
                        + " ORDER BY r.received_at, n.id LIMIT ?",
                Long.class,
                utc(now),
                limit);
    }

    /** Mọi thông báo đang giữ, để tính lại thời điểm gửi khi CFG-M13-02 đổi (FR-024). */
    public List<Long> allHeld() {
        return jdbc.queryForList(
                "SELECT notification_id FROM notification_state WHERE status = 'HELD_FOR_QUIET_HOURS'"
                        + " ORDER BY notification_id",
                Long.class);
    }

    /** Khóa dòng trạng thái rồi đọc; rỗng khi thông báo không còn đang giữ (lượt khác đã xử lý). */
    public Optional<Held> lock(long notificationId) {
        List<String> status = jdbc.queryForList(
                "SELECT status FROM notification_state WHERE notification_id = ? FOR UPDATE",
                String.class,
                notificationId);
        if (status.isEmpty() || !"HELD_FOR_QUIET_HOURS".equals(status.getFirst())) {
            return Optional.empty();
        }
        Set<InformationType> parts = EnumSet.noneOf(InformationType.class);
        jdbc.queryForList(
                        "SELECT c.information_type FROM notification_request_content c"
                                + " JOIN notification n ON n.request_id = c.request_id WHERE n.id = ?",
                        String.class,
                        notificationId)
                .forEach(t -> parts.add(InformationType.valueOf(t)));
        return Optional.of(jdbc.queryForObject(
                "SELECT n.recipient_type, n.recipient_id, n.basis, n.level, r.resident_id, r.information_subject,"
                        + " r.q354_case, s.scheduled_send_at, FIND_IN_SET('INVALID_OUT_OF_APP_CONTENT', r.flags) > 0"
                        + " FROM notification n"
                        + " JOIN notification_request r ON r.id = n.request_id"
                        + " JOIN notification_state s ON s.notification_id = n.id WHERE n.id = ?",
                (rs, i) -> new Held(
                        notificationId,
                        RecipientType.valueOf(rs.getString(1)),
                        rs.getLong(2),
                        json.readValue(rs.getString(3), new TypeReference<List<String>>() {}),
                        NotificationLevel.valueOf(rs.getString(4)),
                        rs.getObject(5, Long.class),
                        Optional.ofNullable(rs.getString(6)).map(InformationSubject::valueOf),
                        rs.getString(7) != null,
                        rs.getBoolean(9),
                        parts,
                        rs.getObject(8, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                notificationId));
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
