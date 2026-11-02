package com.eldercare.backend.notification.internal.governance.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.notification.internal.directcontact.domain.DirectContactContent;
import com.eldercare.backend.notification.internal.domain.DeliveryAttemptContent;
import com.eldercare.backend.notification.internal.domain.NotificationEventContent;
import com.eldercare.backend.notification.internal.persistence.M13Sql;
import com.eldercare.backend.notification.internal.urgent.domain.CallReminderEvents;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ yêu cầu thông báo và mọi thứ thuộc nó ({@code LuuTruProvider}; data-model mục 10, FR-041): theo CFG-M15-04
 * tính từ lúc tiếp nhận. Yêu cầu gắn người cao tuổi giữ theo hồ sơ (CFG-M15-03 từ ngày kết thúc lưu trú), chưa có giao
 * diện của 001 nên chưa hết hạn (như yêu cầu phê duyệt của 000). Yêu cầu không gắn người cao tuổi tiếp nhận sau bản ghi
 * nguồn nên không bị loại bỏ trước bản ghi nguồn cùng hạn.
 */
@Component
class NotificationRequestRetention implements RetentionProvider {

    static final String TYPE = "M13.NOTIFICATION_REQUEST";

    private static final String NOTIFICATIONS = "SELECT id FROM notification WHERE request_id IN ";

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final SharedRowRetention sharedRows;

    NotificationRequestRetention(JdbcTemplate jdbc, RetentionDatabase database, SharedRowRetention sharedRows) {
        this.jdbc = jdbc;
        this.database = database;
        this.sharedRows = sharedRows;
    }

    @Override
    public String dataType() {
        return TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.NotResidentLinked();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return jdbc
                .query(
                        "SELECT id, received_at FROM notification_request WHERE resident_id IS NULL AND received_at < ?"
                                + " ORDER BY received_at, id LIMIT ?",
                        (rs, i) -> new Candidate(
                                rs.getLong(1),
                                rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                        LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                        batch)
                .stream();
    }

    /** Không bản ghi nào của module khác trỏ về yêu cầu thông báo. */
    @Override
    public Set<DataRef> referencesTo(long id) {
        return Set.of();
    }

    /**
     * Xóa yêu cầu cùng thông báo, lượt nhắc gọi (kèm mọi lượt cùng chuỗi gọi) và dòng con ở bảng chung (009 T043,
     * data-model mục 0.1, 6, 10): sự kiện, lần gửi, liên hệ trực tiếp gắn thông báo; kết quả gắn lần gửi, mục liên hệ;
     * thông báo gộp, chuyển nhắc, kết quả gọi gắn lượt. Dấu bản trùng gắn yêu cầu do job loại bỏ xóa.
     */
    @Override
    public void delete(Collection<Long> ids) {
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        JdbcTemplate r = database.jdbc();
        List<Long> notifications = r.queryForList(NOTIFICATIONS + in, Long.class);
        List<Long> reminders = notifications.isEmpty()
                ? List.of()
                : r.queryForList(
                        "SELECT x.id FROM call_reminder x WHERE COALESCE(x.call_chain_id, x.id) IN (SELECT"
                                + " COALESCE(c.call_chain_id, c.id) FROM call_reminder c JOIN record_event l"
                                + " ON l.entity_id = c.id AND l.entity_type = 'M13.CALL_REMINDER'"
                                + " AND l.kind = 'M13.CALL_REMINDER_NOTIFICATION'"
                                + " WHERE JSON_VALUE(l.content, '$.notificationId' RETURNING SIGNED) IN ("
                                + NOTIFICATIONS + in + "))",
                        Long.class);
        List<EntityRef> children = new ArrayList<>();
        notifications.forEach(id -> children.add(new EntityRef(NotificationEventContent.NOTIFICATION, id)));
        reminders.forEach(id -> children.add(new EntityRef(CallReminderEvents.CALL_REMINDER, id)));
        if (!notifications.isEmpty()) {
            String nin = notifications.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
            r.queryForList(
                            "SELECT id FROM record_event a WHERE " + M13Sql.attempts("a") + " AND a.entity_id IN "
                                    + nin,
                            Long.class)
                    .forEach(id -> children.add(new EntityRef(DeliveryAttemptContent.KIND.code(), id)));
            r.queryForList(
                            "SELECT id FROM simple_record WHERE kind = 'M13.DIRECT_CONTACT'"
                                    + " AND entity_type = 'M13.NOTIFICATION' AND entity_id IN " + nin,
                            Long.class)
                    .forEach(id -> children.add(new EntityRef(DirectContactContent.KIND.code(), id)));
        }
        if (!children.isEmpty()) {
            sharedRows.deleteFor(children);
        }
        if (!reminders.isEmpty()) {
            String rin = reminders.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
            r.update("DELETE FROM call_reminder WHERE call_chain_id IS NOT NULL AND id IN " + rin);
            r.update("DELETE FROM call_reminder WHERE id IN " + rin);
        }
        r.update("DELETE FROM notification WHERE request_id IN " + in);
        r.update("DELETE FROM notification_request WHERE id IN " + in);
    }
}
