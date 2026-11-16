package com.eldercare.backend.notification.internal.governance.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.internal.directcontact.domain.DirectContactContent;
import com.eldercare.backend.notification.internal.domain.DeliveryAttemptContent;
import com.eldercare.backend.notification.internal.domain.NotificationEventContent;
import com.eldercare.backend.notification.internal.persistence.M13Sql;
import com.eldercare.backend.notification.internal.urgent.domain.CallReminderEvents;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ yêu cầu thông báo và mọi thứ thuộc nó ({@code LuuTruProvider}; data-model mục 10, FR-041): theo CFG-M15-04
 * tính từ lúc tiếp nhận. Yêu cầu gắn người cao tuổi giữ theo hồ sơ (CFG-M15-03 từ lúc người đó sang trạng thái cuối, đọc
 * qua {@link ResidentApi#finalStatusAt}; 000 T112, 2026-11-16) và đi cùng chuỗi của hồ sơ ({@link #referencing}). Yêu cầu không
 * gắn người cao tuổi tiếp nhận sau bản ghi nguồn nên không bị loại bỏ trước bản ghi nguồn cùng hạn.
 */
@Component
class NotificationRequestRetention implements RetentionProvider {

    static final String TYPE = "M13.NOTIFICATION_REQUEST";

    private static final String NOTIFICATIONS = "SELECT id FROM notification WHERE request_id IN ";

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final SharedRowRetention sharedRows;
    private final ObjectProvider<ResidentApi> residents;
    private final ParameterApi parameters;
    private final BusinessTime time;

    NotificationRequestRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            SharedRowRetention sharedRows,
            ObjectProvider<ResidentApi> residents,
            ParameterApi parameters,
            BusinessTime time) {
        this.jdbc = jdbc;
        this.database = database;
        this.sharedRows = sharedRows;
        this.residents = residents;
        this.parameters = parameters;
        this.time = time;
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
        return expiredCandidates(cutoff, null, batch);
    }

    /** Trang sau mốc {@code (received_at, id)} (000 T112). */
    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
        LocalDateTime afterAt = after == null
                ? LocalDateTime.of(1000, 1, 1, 0, 0)
                : LocalDateTime.ofInstant(after.at(), ZoneOffset.UTC);
        long afterId = after == null ? 0 : after.id();
        List<Candidate> unlinked = jdbc.query(
                "SELECT id, received_at FROM notification_request WHERE resident_id IS NULL AND received_at < ?"
                        + " AND (received_at > ? OR (received_at = ? AND id > ?))"
                        + " ORDER BY received_at, id LIMIT ?",
                (rs, i) -> new Candidate(
                        rs.getLong(1), rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                afterAt,
                afterAt,
                afterId,
                batch);
        return RetentionProvider.page(Stream.concat(unlinked.stream(), residentLinked().stream()), after, batch);
    }

    /**
     * Yêu cầu gắn hồ sơ đã hết hạn theo hồ sơ: người cao tuổi sang trạng thái cuối trước mốc CFG-M15-03 (000 T112); mốc
     * của ứng viên là lúc sang trạng thái cuối. Chưa có nguồn trạng thái cuối thì giữ.
     */
    private List<Candidate> residentLinked() {
        ResidentApi source = residents.getIfAvailable();
        if (source == null) return List.of();
        Instant profileCutoff = time.now()
                .atZone(BusinessTime.ZONE)
                .minus(parameters.read(ParameterCode.CFG_M15_03).asPeriod())
                .toInstant();
        List<Candidate> found = new ArrayList<>();
        jdbc.query("SELECT id, resident_id FROM notification_request WHERE resident_id IS NOT NULL", rs -> {
            long id = rs.getLong(1);
            source.finalStatusAt(rs.getLong(2))
                    .filter(at -> at.isBefore(profileCutoff))
                    .ifPresent(at -> found.add(new Candidate(id, at)));
        });
        return found;
    }

    /** Yêu cầu thông báo gắn hồ sơ người cao tuổi {@code target} (000 T112, FR-051a). */
    @Override
    public Set<DataRef> referencing(DataRef target) {
        if (!SharedRowRetention.RESIDENT.equals(target.dataType())) return Set.of();
        return jdbc
                .queryForList("SELECT id FROM notification_request WHERE resident_id = ?", Long.class, target.id())
                .stream()
                .map(id -> new DataRef(TYPE, id))
                .collect(Collectors.toSet());
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
