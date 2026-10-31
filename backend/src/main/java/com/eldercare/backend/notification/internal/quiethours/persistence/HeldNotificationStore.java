package com.eldercare.backend.notification.internal.quiethours.persistence;

import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.quiethours.domain.HeldNotification;
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

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    HeldNotificationStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Id thông báo đang giữ có thời điểm gửi dự kiến tới {@code now}, theo thứ tự tiếp nhận yêu cầu (FR-025). */
    public List<Long> due(Instant now, int limit) {
        return jdbc.queryForList(
                "SELECT n.id FROM notification n JOIN notification_request r ON r.id = n.request_id"
                        + " WHERE n.status = 'HELD_FOR_QUIET_HOURS' AND n.scheduled_send_at <= ?"
                        + " ORDER BY r.received_at, n.id LIMIT ?",
                Long.class,
                utc(now),
                limit);
    }

    /** Mọi thông báo đang giữ, để tính lại thời điểm gửi khi CFG-M13-02 đổi (FR-024). */
    public List<Long> allHeld() {
        return jdbc.queryForList(
                "SELECT id FROM notification WHERE status = 'HELD_FOR_QUIET_HOURS' ORDER BY id", Long.class);
    }

    /** Thông báo người thân đang giữ về người cao tuổi này (FR-015, FR-025: qua đời trước giờ gửi). */
    public List<Long> heldForFamilyOf(long residentId) {
        return jdbc.queryForList(
                "SELECT n.id FROM notification n JOIN notification_request r ON r.id = n.request_id"
                        + " WHERE n.status = 'HELD_FOR_QUIET_HOURS' AND n.recipient_type = 'FAMILY_MEMBER'"
                        + " AND r.resident_id = ? ORDER BY r.received_at, n.id",
                Long.class,
                residentId);
    }

    /** Khóa dòng thông báo rồi đọc; rỗng khi thông báo không còn đang giữ (lượt khác đã xử lý). */
    public Optional<HeldNotification> lock(long notificationId) {
        List<String> status = jdbc.queryForList(
                "SELECT status FROM notification WHERE id = ? FOR UPDATE", String.class, notificationId);
        if (status.isEmpty() || !"HELD_FOR_QUIET_HOURS".equals(status.getFirst())) {
            return Optional.empty();
        }
        Set<InformationType> parts = EnumSet.noneOf(InformationType.class);
        jdbc.queryForList(
                        "SELECT c.information_type FROM notification n JOIN notification_request r"
                                + " ON r.id = n.request_id, JSON_TABLE(r.contents, '$[*]' COLUMNS"
                                + " (information_type VARCHAR(20) PATH '$.informationType')) c WHERE n.id = ?",
                        String.class,
                        notificationId)
                .forEach(t -> parts.add(InformationType.valueOf(t)));
        return Optional.of(jdbc.queryForObject(
                "SELECT n.recipient_type, n.recipient_id, n.basis, n.level, r.resident_id, r.information_subject,"
                        + " r.q354_case, n.scheduled_send_at, FIND_IN_SET('INVALID_OUT_OF_APP_CONTENT', r.flags) > 0,"
                        + " FIND_IN_SET('AFTER_DEATH', r.flags) > 0"
                        + " FROM notification n"
                        + " JOIN notification_request r ON r.id = n.request_id WHERE n.id = ?",
                (rs, i) -> new HeldNotification(
                        notificationId,
                        RecipientType.valueOf(rs.getString(1)),
                        rs.getLong(2),
                        json.readValue(rs.getString(3), new TypeReference<List<String>>() {}),
                        NotificationLevel.valueOf(rs.getString(4)),
                        rs.getObject(5, Long.class),
                        Optional.ofNullable(rs.getString(6)).map(InformationSubject::valueOf),
                        rs.getString(7) != null,
                        rs.getBoolean(9),
                        rs.getBoolean(10),
                        parts,
                        rs.getObject(8, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                notificationId));
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
