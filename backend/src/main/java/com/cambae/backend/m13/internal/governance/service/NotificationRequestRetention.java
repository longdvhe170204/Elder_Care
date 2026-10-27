package com.cambae.backend.m13.internal.governance.service;

import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private static final String ATTEMPTS = "SELECT a.id FROM delivery_attempt a JOIN notification n ON n.id ="
            + " a.notification_id WHERE n.request_id IN ";

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;

    NotificationRequestRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        this.jdbc = jdbc;
        this.database = database;
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

    @Override
    public void delete(Collection<Long> ids) {
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        JdbcTemplate r = database.jdbc();
        // Lượt nhắc gọi của các thông báo, kèm mọi lượt cùng chuỗi gọi (data-model mục 6, 10)
        List<Long> reminders = r.queryForList(
                "SELECT x.id FROM call_reminder x WHERE COALESCE(x.call_chain_id, x.id) IN (SELECT"
                        + " COALESCE(c.call_chain_id, c.id) FROM call_reminder c JOIN call_reminder_notification l"
                        + " ON l.call_reminder_id = c.id WHERE l.notification_id IN (" + NOTIFICATIONS + in + "))",
                Long.class);
        if (!reminders.isEmpty()) {
            String rin = reminders.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
            r.update("DELETE FROM call_result WHERE call_reminder_id IN " + rin);
            r.update("DELETE FROM call_reminder_handoff WHERE call_reminder_id IN " + rin);
            r.update("DELETE FROM call_reminder_state WHERE call_reminder_id IN " + rin);
            r.update("DELETE FROM call_reminder_notification WHERE call_reminder_id IN " + rin);
            r.update("DELETE FROM call_reminder WHERE call_chain_id IS NOT NULL AND id IN " + rin);
            r.update("DELETE FROM call_reminder WHERE id IN " + rin);
        }
        r.update("DELETE FROM direct_contact_result WHERE direct_contact_id IN (SELECT id FROM direct_contact"
                + " WHERE notification_id IN (" + NOTIFICATIONS + in + "))");
        r.update("DELETE FROM direct_contact WHERE notification_id IN (" + NOTIFICATIONS + in + ")");
        r.update("DELETE FROM delivery_result WHERE attempt_id IN (" + ATTEMPTS + in + ")");
        r.update("DELETE FROM delivery_attempt WHERE notification_id IN (" + NOTIFICATIONS + in + ")");
        r.update("DELETE FROM notification_event WHERE notification_id IN (" + NOTIFICATIONS + in + ")");
        r.update("DELETE FROM notification_state WHERE notification_id IN (" + NOTIFICATIONS + in + ")");
        r.update("DELETE FROM notification WHERE request_id IN " + in);
        r.update("DELETE FROM notification_request_content WHERE request_id IN " + in);
        r.update("DELETE FROM notification_request_recipient_group WHERE request_id IN " + in);
        r.update("DELETE FROM notification_request_duplicate WHERE request_id IN " + in);
        r.update("DELETE FROM notification_request WHERE id IN " + in);
    }
}
