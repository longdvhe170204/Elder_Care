package com.eldercare.backend.notification.internal.urgent.persistence;

import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.domain.ResidentFloor;
import com.eldercare.backend.notification.internal.persistence.M13SharedRows;
import com.eldercare.backend.notification.internal.persistence.M13Sql;
import com.eldercare.backend.notification.internal.urgent.domain.CallReminderEvents;
import com.eldercare.backend.notification.internal.urgent.domain.CallReminderHandoff;
import com.eldercare.backend.notification.internal.urgent.domain.CallReminderTarget.Tier;
import com.eldercare.backend.notification.internal.urgent.domain.CallResultRow;
import com.eldercare.backend.notification.internal.urgent.domain.ContentItem;
import com.eldercare.backend.notification.internal.urgent.domain.DueNotification;
import com.eldercare.backend.notification.internal.urgent.domain.Reminder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lượt nhắc gọi (trạng thái dẫn xuất trên chính dòng lượt) và dòng con của lượt ở {@code record_event}: thông báo gộp,
 * chuyển nhắc, kết quả gọi (009 T043, data-model mục 0.1, 6).
 */
@Repository
public class CallReminderStore {

    private static final String REMINDER = "SELECT r.id, COALESCE(r.call_chain_id, r.id) AS chain_id, r.callee_type,"
            + " r.callee_id, r.resident_id, r.floor_area_id, r.source_record_type, r.source_record_id, r.status,"
            + " r.final_result, r.stop_reason, r.due_at, r.current_staff_id,"
            + " (SELECT JSON_VALUE(h.content, '$.tier') FROM record_event h WHERE "
            + child("h", "M13.CALL_REMINDER_HANDOFF")
            + " AND h.entity_id = r.id ORDER BY h.id DESC LIMIT 1) AS tier FROM call_reminder r";

    private static final String LINK = child("l", "M13.CALL_REMINDER_NOTIFICATION");
    private static final String LINKED_NOTIFICATION = "JSON_VALUE(l.content, '$.notificationId' RETURNING SIGNED)";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final M13SharedRows shared;

    CallReminderStore(JdbcTemplate jdbc, JsonMapper json, M13SharedRows shared) {
        this.jdbc = jdbc;
        this.json = json;
        this.shared = shared;
    }

    /**
     * Thông báo Khẩn cấp đã gửi, chưa xác nhận, chưa thuộc lượt nào, của nhân viên hoặc người thân: quá hạn xác nhận hoặc
     * mọi lần gửi đều không thành công (kể cả không có kênh nào). Thông báo do chính M13 gửi (nhắc gọi, báo Quản lý viện)
     * không sinh lượt nhắc.
     */
    public List<DueNotification> due(Instant sentBefore, int limit) {
        String noAttempt =
                "NOT EXISTS (SELECT 1 FROM record_event a WHERE " + M13Sql.attempts("a") + " AND a.entity_id = n.id)";
        return jdbc.query(
                "SELECT n.id, n.recipient_type, n.recipient_id, q.resident_id, q.floor_area_id, q.source_record_type,"
                        + " q.source_record_id, (n.status = 'DELIVERY_FAILED' OR " + noAttempt + ") AS failed"
                        + " FROM notification n JOIN notification_request q ON q.id = n.request_id"
                        + " WHERE n.level = 'URGENT' AND n.sent_at IS NOT NULL AND n.acknowledged_at IS NULL"
                        + " AND (n.status IS NULL OR n.status <> 'UNREACHABLE')"
                        + " AND n.recipient_type IN ('STAFF', 'FAMILY_MEMBER')"
                        + " AND (q.source_module IS NULL OR q.source_module <> 'M13')"
                        + " AND NOT EXISTS (SELECT 1 FROM record_event l WHERE l.unique_key = CONCAT('M13.CRN:', n.id))"
                        + " AND (n.sent_at <= ? OR n.status = 'DELIVERY_FAILED' OR " + noAttempt + ")"
                        + " ORDER BY n.sent_at, n.id LIMIT ?",
                (rs, i) -> new DueNotification(
                        rs.getLong(1),
                        RecipientType.valueOf(rs.getString(2)),
                        rs.getLong(3),
                        rs.getObject(4, Long.class),
                        rs.getObject(5, Long.class),
                        rs.getString(6),
                        rs.getString(7),
                        rs.getBoolean(8)),
                utc(sentBefore),
                limit);
    }

    /** Thông báo đã thuộc một lượt (FR-032). */
    public boolean linked(long notificationId) {
        return !remindersOf(notificationId).isEmpty();
    }

    /** Lượt Chờ kết quả của (người cần gọi, người cao tuổi hoặc bản ghi nguồn), khóa dòng (FR-027a, FR-032). */
    public Optional<Long> lockPending(String calleeKey) {
        return jdbc
                .queryForList("SELECT id FROM call_reminder WHERE pending_key = ? FOR UPDATE", Long.class, calleeKey)
                .stream()
                .findFirst();
    }

    /** Lượt mới ở trạng thái Chờ kết quả, nhắc {@code currentStaffId} tới hạn {@code dueAt}. */
    public long insertReminder(
            Long chainId,
            RecipientType calleeType,
            long calleeId,
            Long residentId,
            String sourceRecordType,
            String sourceRecordId,
            Long floorAreaId,
            String creationReason,
            Instant dueAt,
            long currentStaffId,
            String calleeKey,
            Instant at) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO call_reminder (call_chain_id, callee_type, callee_id, resident_id,"
                                    + " source_record_type, source_record_id, floor_area_id, creation_reason, created_at,"
                                    + " status, due_at, current_staff_id, callee_key)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'AWAITING_RESULT', ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setObject(1, chainId);
                    ps.setString(2, calleeType.name());
                    ps.setLong(3, calleeId);
                    ps.setObject(4, residentId);
                    ps.setString(5, residentId == null ? sourceRecordType : null);
                    ps.setString(6, residentId == null ? sourceRecordId : null);
                    ps.setObject(7, floorAreaId);
                    ps.setString(8, creationReason);
                    ps.setObject(9, utc(at));
                    ps.setObject(10, utc(dueAt));
                    ps.setLong(11, currentStaffId);
                    ps.setString(12, calleeKey);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    public void link(long reminderId, long notificationId, Instant at) {
        shared.addEvent(
                CallReminderEvents.Link.KIND,
                CallReminderEvents.CALL_REMINDER,
                reminderId,
                new CallReminderEvents.Link(notificationId),
                null,
                at,
                CallReminderEvents.Link.uniqueKey(notificationId));
    }

    /** Ghi một lần chuyển nhắc; trả id (khóa sự kiện của thông báo nhắc). */
    public long handoff(
            long reminderId,
            Long previousStaffId,
            long remindedStaffId,
            Tier tier,
            String reason,
            Instant dueAt,
            Instant at) {
        return shared.addEvent(
                CallReminderEvents.Handoff.KIND,
                CallReminderEvents.CALL_REMINDER,
                reminderId,
                new CallReminderEvents.Handoff(previousStaffId, remindedStaffId, tier, reason, dueAt),
                null,
                at,
                null);
    }

    public void reassign(long reminderId, long currentStaffId, Instant dueAt) {
        jdbc.update(
                "UPDATE call_reminder SET current_staff_id = ?, due_at = ? WHERE id = ?",
                currentStaffId,
                utc(dueAt),
                reminderId);
    }

    public void finish(long reminderId, String status, String finalResult, String stopReason) {
        jdbc.update(
                "UPDATE call_reminder SET status = ?, final_result = ?, stop_reason = ? WHERE id = ?",
                status,
                finalResult,
                stopReason,
                reminderId);
    }

    /** Khóa dòng lượt rồi đọc lượt. */
    public Optional<Reminder> lock(long reminderId) {
        jdbc.queryForList("SELECT id FROM call_reminder WHERE id = ? FOR UPDATE", Long.class, reminderId);
        return find(reminderId);
    }

    public Optional<Reminder> find(long reminderId) {
        return jdbc.query(REMINDER + " WHERE r.id = ?", CallReminderStore::reminder, reminderId).stream()
                .findFirst();
    }

    /** Các lượt đang Chờ kết quả (chuyển nhắc, lượt của tôi). */
    public List<Reminder> awaiting() {
        return jdbc.query(REMINDER + " WHERE r.status = 'AWAITING_RESULT' ORDER BY r.id", CallReminderStore::reminder);
    }

    /** Lượt Chờ kết quả có thông báo này. */
    public Optional<Long> awaitingWith(long notificationId) {
        return jdbc
                .queryForList(
                        "SELECT r.id FROM record_event l JOIN call_reminder r ON r.id = l.entity_id"
                                + " WHERE l.unique_key = ? AND r.status = 'AWAITING_RESULT'",
                        Long.class,
                        CallReminderEvents.Link.uniqueKey(notificationId))
                .stream()
                .findFirst();
    }

    /** Thông báo gộp vào các lượt của chuỗi gọi (FR-029a). */
    public List<Long> chainNotifications(long chainId) {
        return jdbc.queryForList(
                "SELECT " + LINKED_NOTIFICATION + " FROM record_event l JOIN call_reminder r ON r.id = l.entity_id"
                        + " WHERE " + LINK + " AND COALESCE(r.call_chain_id, r.id) = ? ORDER BY l.id",
                Long.class,
                chainId);
    }

    /** Người thân đã được nhắc gọi trong chuỗi (không nhắc lại cùng người, FR-029a). */
    public List<Long> chainFamilyCallees(long chainId) {
        return jdbc.queryForList(
                "SELECT callee_id FROM call_reminder WHERE COALESCE(call_chain_id, id) = ? AND callee_type = 'FAMILY_MEMBER'",
                Long.class,
                chainId);
    }

    public boolean chainAwaiting(long chainId) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM call_reminder WHERE COALESCE(call_chain_id, id) = ?"
                                + " AND status = 'AWAITING_RESULT'",
                        Integer.class,
                        chainId)
                > 0;
    }

    /** Số thông báo của chuỗi chưa được xác nhận. */
    public int chainUnacknowledged(long chainId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_event l JOIN call_reminder r ON r.id = l.entity_id"
                        + " JOIN notification n ON n.id = " + LINKED_NOTIFICATION
                        + " WHERE " + LINK + " AND COALESCE(r.call_chain_id, r.id) = ? AND n.acknowledged_at IS NULL",
                Integer.class,
                chainId);
    }

    /** Nội dung cần báo của chuỗi theo thứ tự thời gian (FR-027a). */
    public List<ContentItem> chainContent(long chainId) {
        return jdbc.query(
                "SELECT n.id, COALESCE(q.occurred_at, q.received_at), q.short_content FROM record_event l"
                        + " JOIN call_reminder r ON r.id = l.entity_id JOIN notification n ON n.id = "
                        + LINKED_NOTIFICATION + " JOIN notification_request q ON q.id = n.request_id"
                        + " WHERE " + LINK + " AND COALESCE(r.call_chain_id, r.id) = ?"
                        + " ORDER BY COALESCE(q.occurred_at, q.received_at), n.id",
                (rs, i) -> new ContentItem(rs.getLong(1), instant(rs, 2), rs.getString(3)),
                chainId);
    }

    public List<CallReminderHandoff> handoffs(long reminderId) {
        return jdbc.query(
                "SELECT h.content, h.occurred_at FROM record_event h WHERE " + child("h", "M13.CALL_REMINDER_HANDOFF")
                        + " AND h.entity_id = ? ORDER BY h.id",
                (rs, i) -> {
                    CallReminderEvents.Handoff h = json.readValue(rs.getString(1), CallReminderEvents.Handoff.class);
                    return new CallReminderHandoff(
                            h.previousStaffId(), h.remindedStaffId(), h.tier(), h.reason(), h.dueAt(), instant(rs, 2));
                },
                reminderId);
    }

    public long insertResult(
            long reminderId,
            long calledBy,
            Instant calledAt,
            String calledPhoneNumber,
            String result,
            String reason,
            String answeredBy,
            String note,
            boolean supplementary,
            Instant at) {
        return shared.addEvent(
                CallReminderEvents.Result.KIND,
                CallReminderEvents.CALL_REMINDER,
                reminderId,
                new CallReminderEvents.Result(
                        calledAt, calledPhoneNumber, result, reason, answeredBy, note, supplementary),
                calledBy,
                at,
                null);
    }

    public List<CallResultRow> results(long reminderId) {
        return jdbc.query(
                "SELECT c.id, c.actor_id, c.content FROM record_event c WHERE " + child("c", "M13.CALL_RESULT")
                        + " AND c.entity_id = ? ORDER BY c.id",
                this::result,
                reminderId);
    }

    /** Kết quả gọi của mọi lượt trong chuỗi, cho thông báo Quản lý viện khi hết người (FR-029a). */
    public List<CallResultRow> chainResults(long chainId) {
        return jdbc
                .query(
                        "SELECT c.id, c.actor_id, c.content FROM record_event c JOIN call_reminder r"
                                + " ON r.id = c.entity_id WHERE " + child("c", "M13.CALL_RESULT")
                                + " AND COALESCE(r.call_chain_id, r.id) = ?",
                        this::result,
                        chainId)
                .stream()
                .sorted(Comparator.comparing(CallResultRow::calledAt).thenComparingLong(CallResultRow::id))
                .toList();
    }

    /** Lượt Chờ kết quả của chuỗi gọi. */
    public List<Long> awaitingInChain(long chainId) {
        return jdbc.queryForList(
                "SELECT id FROM call_reminder WHERE COALESCE(call_chain_id, id) = ? AND status = 'AWAITING_RESULT'"
                        + " ORDER BY id",
                Long.class,
                chainId);
    }

    /** Bản ghi nguồn của thông báo, để báo module nguồn (FR-035). */
    public SourceRecord source(long notificationId) {
        return jdbc.queryForObject(
                "SELECT q.source_record_type, q.source_record_id FROM notification n JOIN notification_request q"
                        + " ON q.id = n.request_id WHERE n.id = ?",
                (rs, i) -> new SourceRecord(rs.getString(1), rs.getString(2)),
                notificationId);
    }

    /** Thông báo gộp vào một lượt (FR-030). */
    public List<Long> notificationsOf(long reminderId) {
        return jdbc.queryForList(
                "SELECT " + LINKED_NOTIFICATION + " FROM record_event l WHERE " + LINK + " AND l.entity_id = ?"
                        + " ORDER BY l.id",
                Long.class,
                reminderId);
    }

    /** Mọi lượt của các chuỗi gọi có thông báo này, theo thứ tự tạo (tra cứu cho module nguồn, FR-031). */
    public List<Long> chainRemindersOf(long notificationId) {
        return jdbc.queryForList(
                "SELECT x.id FROM call_reminder x WHERE COALESCE(x.call_chain_id, x.id) IN (SELECT"
                        + " COALESCE(r.call_chain_id, r.id) FROM call_reminder r JOIN record_event l ON l.entity_id = r.id"
                        + " WHERE l.unique_key = ?) ORDER BY x.id",
                Long.class,
                CallReminderEvents.Link.uniqueKey(notificationId));
    }

    /** Lượt có thông báo này. */
    public List<Long> remindersOf(long notificationId) {
        return jdbc.queryForList(
                "SELECT entity_id FROM record_event WHERE unique_key = ?",
                Long.class,
                CallReminderEvents.Link.uniqueKey(notificationId));
    }

    /** Kết quả gọi {@code id} (đích của đính chính {@code M13.CALL_RESULT}). */
    public Optional<CallResultRow> callResult(long id) {
        return jdbc
                .query(
                        "SELECT c.id, c.actor_id, c.content FROM record_event c WHERE c.id = ? AND c.kind = 'M13.CALL_RESULT'",
                        this::result,
                        id)
                .stream()
                .findFirst();
    }

    /** Lượt nhắc gọi của kết quả gọi {@code id}: người cao tuổi, tầng/khu vực. */
    public Optional<ResidentFloor> callResultScope(long id) {
        return jdbc
                .query(
                        "SELECT r.resident_id, r.floor_area_id FROM record_event c JOIN call_reminder r ON r.id = c.entity_id"
                                + " WHERE c.id = ? AND " + child("c", "M13.CALL_RESULT"),
                        (rs, i) -> new ResidentFloor(rs.getObject(1, Long.class), rs.getObject(2, Long.class)),
                        id)
                .stream()
                .findFirst();
    }

    private static String child(String alias, String kind) {
        return alias + ".entity_type = '" + CallReminderEvents.CALL_REMINDER + "' AND " + alias + ".kind = '" + kind
                + "'";
    }

    private static Reminder reminder(ResultSet rs, int i) throws SQLException {
        return new Reminder(
                rs.getLong("id"),
                rs.getLong("chain_id"),
                RecipientType.valueOf(rs.getString("callee_type")),
                rs.getLong("callee_id"),
                rs.getObject("resident_id", Long.class),
                rs.getObject("floor_area_id", Long.class),
                rs.getString("source_record_type"),
                rs.getString("source_record_id"),
                rs.getString("status"),
                rs.getString("final_result"),
                rs.getString("stop_reason"),
                rs.getObject("due_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getLong("current_staff_id"),
                Tier.valueOf(rs.getString("tier")));
    }

    private CallResultRow result(ResultSet rs, int i) throws SQLException {
        CallReminderEvents.Result r = json.readValue(rs.getString(3), CallReminderEvents.Result.class);
        return new CallResultRow(
                rs.getLong(1),
                rs.getLong(2),
                r.calledAt(),
                r.calledPhoneNumber(),
                r.result(),
                r.reason(),
                r.answeredBy(),
                r.note(),
                r.supplementary());
    }

    private static Instant instant(ResultSet rs, int column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class).toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
