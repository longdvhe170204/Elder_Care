package com.cambae.backend.m13.internal.urgent.persistence;

import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.internal.urgent.domain.CallReminderTarget.Tier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Lượt nhắc gọi, thông báo gộp, chuyển nhắc, trạng thái, kết quả gọi (V009_04; data-model mục 6). */
@Repository
public class CallReminderStore {

    /** Thông báo Khẩn cấp chưa xác nhận cần lượt nhắc gọi (FR-027, FR-028). */
    public record DueNotification(
            long notificationId,
            RecipientType calleeType,
            long calleeId,
            Long residentId,
            Long floorAreaId,
            String sourceRecordType,
            String sourceRecordId,
            boolean deliveryFailed) {}

    /** Một lượt với trạng thái hiện hành và bậc của người đang được nhắc. */
    public record Reminder(
            long id,
            long chainId,
            RecipientType calleeType,
            long calleeId,
            Long residentId,
            Long floorAreaId,
            String sourceRecordType,
            String sourceRecordId,
            String status,
            String finalResult,
            String stopReason,
            Instant dueAt,
            long currentStaffId,
            Tier tier) {}

    public record Handoff(
            Long previousStaffId, long remindedStaffId, Tier tier, String reason, Instant dueAt, Instant at) {}

    public record CallResultRow(
            long id,
            long calledBy,
            Instant calledAt,
            String calledPhoneNumber,
            String result,
            String reason,
            String answeredBy,
            String note,
            boolean supplementary) {}

    /** Nội dung cần báo của một thông báo gộp (FR-027). */
    public record ContentItem(long notificationId, Instant occurredAt, String content) {}

    private static final String REMINDER = "SELECT r.id, COALESCE(r.call_chain_id, r.id) AS chain_id, r.callee_type,"
            + " r.callee_id, r.resident_id, r.floor_area_id, r.source_record_type, r.source_record_id, s.status,"
            + " s.final_result, s.stop_reason, s.due_at, s.current_staff_id,"
            + " (SELECT h.tier FROM call_reminder_handoff h WHERE h.call_reminder_id = r.id ORDER BY h.id DESC LIMIT 1)"
            + " AS tier FROM call_reminder r JOIN call_reminder_state s ON s.call_reminder_id = r.id";

    private final JdbcTemplate jdbc;

    CallReminderStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Thông báo Khẩn cấp đã gửi, chưa xác nhận, chưa thuộc lượt nào, của nhân viên hoặc người thân: quá hạn xác nhận hoặc
     * mọi lần gửi đều không thành công (kể cả không có kênh nào). Thông báo do chính M13 gửi (nhắc gọi, báo Quản lý viện)
     * không sinh lượt nhắc.
     */
    public List<DueNotification> due(Instant sentBefore, int limit) {
        return jdbc.query(
                "SELECT n.id, n.recipient_type, n.recipient_id, q.resident_id, q.floor_area_id, q.source_record_type,"
                        + " q.source_record_id, (s.status = 'DELIVERY_FAILED' OR NOT EXISTS (SELECT 1 FROM"
                        + " delivery_attempt a WHERE a.notification_id = n.id)) AS failed"
                        + " FROM notification_state s JOIN notification n ON n.id = s.notification_id"
                        + " JOIN notification_request q ON q.id = n.request_id"
                        + " WHERE s.level = 'URGENT' AND s.sent_at IS NOT NULL AND s.acknowledged_at IS NULL"
                        + " AND (s.status IS NULL OR s.status <> 'UNREACHABLE')"
                        + " AND n.recipient_type IN ('STAFF', 'FAMILY_MEMBER')"
                        + " AND (q.source_module IS NULL OR q.source_module <> 'M13')"
                        + " AND NOT EXISTS (SELECT 1 FROM call_reminder_notification l WHERE l.notification_id = n.id)"
                        + " AND (s.sent_at <= ? OR s.status = 'DELIVERY_FAILED'"
                        + " OR NOT EXISTS (SELECT 1 FROM delivery_attempt a WHERE a.notification_id = n.id))"
                        + " ORDER BY s.sent_at, n.id LIMIT ?",
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
        return !jdbc.queryForList(
                        "SELECT id FROM call_reminder_notification WHERE notification_id = ?",
                        Long.class,
                        notificationId)
                .isEmpty();
    }

    /** Lượt Chờ kết quả của (người cần gọi, người cao tuổi hoặc bản ghi nguồn), khóa dòng (FR-027a, FR-032). */
    public Optional<Long> lockPending(String calleeKey) {
        return jdbc
                .queryForList(
                        "SELECT call_reminder_id FROM call_reminder_state WHERE pending_key = ? FOR UPDATE",
                        Long.class,
                        calleeKey)
                .stream()
                .findFirst();
    }

    public long insertReminder(
            Long chainId,
            RecipientType calleeType,
            long calleeId,
            Long residentId,
            String sourceRecordType,
            String sourceRecordId,
            Long floorAreaId,
            String creationReason,
            Instant at) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO call_reminder (call_chain_id, callee_type, callee_id, resident_id,"
                                    + " source_record_type, source_record_id, floor_area_id, creation_reason, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    public void insertState(long reminderId, Instant dueAt, long currentStaffId, String calleeKey) {
        jdbc.update(
                "INSERT INTO call_reminder_state (call_reminder_id, status, due_at, current_staff_id, callee_key)"
                        + " VALUES (?, 'AWAITING_RESULT', ?, ?, ?)",
                reminderId,
                utc(dueAt),
                currentStaffId,
                calleeKey);
    }

    public void link(long reminderId, long notificationId, Instant at) {
        jdbc.update(
                "INSERT INTO call_reminder_notification (call_reminder_id, notification_id, merged_at) VALUES (?, ?, ?)",
                reminderId,
                notificationId,
                utc(at));
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
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO call_reminder_handoff (call_reminder_id, previous_staff_id, reminded_staff_id,"
                                    + " tier, reason, due_at, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, reminderId);
                    ps.setObject(2, previousStaffId);
                    ps.setLong(3, remindedStaffId);
                    ps.setString(4, tier.name());
                    ps.setString(5, reason);
                    ps.setObject(6, utc(dueAt));
                    ps.setObject(7, utc(at));
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    public void reassign(long reminderId, long currentStaffId, Instant dueAt) {
        jdbc.update(
                "UPDATE call_reminder_state SET current_staff_id = ?, due_at = ? WHERE call_reminder_id = ?",
                currentStaffId,
                utc(dueAt),
                reminderId);
    }

    public void finish(long reminderId, String status, String finalResult, String stopReason) {
        jdbc.update(
                "UPDATE call_reminder_state SET status = ?, final_result = ?, stop_reason = ? WHERE call_reminder_id = ?",
                status,
                finalResult,
                stopReason,
                reminderId);
    }

    /** Khóa trạng thái lượt rồi đọc lượt. */
    public Optional<Reminder> lock(long reminderId) {
        jdbc.queryForList(
                "SELECT call_reminder_id FROM call_reminder_state WHERE call_reminder_id = ? FOR UPDATE",
                Long.class,
                reminderId);
        return find(reminderId);
    }

    public Optional<Reminder> find(long reminderId) {
        return jdbc.query(REMINDER + " WHERE r.id = ?", CallReminderStore::reminder, reminderId).stream()
                .findFirst();
    }

    /** Các lượt đang Chờ kết quả (chuyển nhắc, lượt của tôi). */
    public List<Reminder> awaiting() {
        return jdbc.query(REMINDER + " WHERE s.status = 'AWAITING_RESULT' ORDER BY r.id", CallReminderStore::reminder);
    }

    /** Lượt Chờ kết quả có thông báo này. */
    public Optional<Long> awaitingWith(long notificationId) {
        return jdbc
                .queryForList(
                        "SELECT l.call_reminder_id FROM call_reminder_notification l JOIN call_reminder_state s"
                                + " ON s.call_reminder_id = l.call_reminder_id"
                                + " WHERE l.notification_id = ? AND s.status = 'AWAITING_RESULT'",
                        Long.class,
                        notificationId)
                .stream()
                .findFirst();
    }

    /** Thông báo gộp vào các lượt của chuỗi gọi (FR-029a). */
    public List<Long> chainNotifications(long chainId) {
        return jdbc.queryForList(
                "SELECT l.notification_id FROM call_reminder_notification l JOIN call_reminder r"
                        + " ON r.id = l.call_reminder_id WHERE COALESCE(r.call_chain_id, r.id) = ? ORDER BY l.id",
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
                        "SELECT COUNT(*) FROM call_reminder r JOIN call_reminder_state s ON s.call_reminder_id = r.id"
                                + " WHERE COALESCE(r.call_chain_id, r.id) = ? AND s.status = 'AWAITING_RESULT'",
                        Integer.class,
                        chainId)
                > 0;
    }

    /** Số thông báo của chuỗi chưa được xác nhận. */
    public int chainUnacknowledged(long chainId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM call_reminder_notification l JOIN call_reminder r ON r.id = l.call_reminder_id"
                        + " JOIN notification_state s ON s.notification_id = l.notification_id"
                        + " WHERE COALESCE(r.call_chain_id, r.id) = ? AND s.acknowledged_at IS NULL",
                Integer.class,
                chainId);
    }

    /** Nội dung cần báo của chuỗi theo thứ tự thời gian (FR-027a). */
    public List<ContentItem> chainContent(long chainId) {
        return jdbc.query(
                "SELECT n.id, COALESCE(q.occurred_at, q.received_at), q.short_content FROM call_reminder_notification l"
                        + " JOIN call_reminder r ON r.id = l.call_reminder_id JOIN notification n ON n.id = l.notification_id"
                        + " JOIN notification_request q ON q.id = n.request_id WHERE COALESCE(r.call_chain_id, r.id) = ?"
                        + " ORDER BY COALESCE(q.occurred_at, q.received_at), n.id",
                (rs, i) -> new ContentItem(rs.getLong(1), instant(rs, 2), rs.getString(3)),
                chainId);
    }

    public List<Handoff> handoffs(long reminderId) {
        return jdbc.query(
                "SELECT previous_staff_id, reminded_staff_id, tier, reason, due_at, occurred_at FROM call_reminder_handoff"
                        + " WHERE call_reminder_id = ? ORDER BY id",
                (rs, i) -> new Handoff(
                        rs.getObject(1, Long.class),
                        rs.getLong(2),
                        Tier.valueOf(rs.getString(3)),
                        rs.getString(4),
                        instant(rs, 5),
                        instant(rs, 6)),
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
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO call_result (call_reminder_id, called_by, called_at, called_phone_number, result,"
                                    + " reason, answered_by, note, supplementary, recorded_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, reminderId);
                    ps.setLong(2, calledBy);
                    ps.setObject(3, utc(calledAt));
                    ps.setString(4, calledPhoneNumber);
                    ps.setString(5, result);
                    ps.setString(6, reason);
                    ps.setString(7, answeredBy);
                    ps.setString(8, note);
                    ps.setBoolean(9, supplementary);
                    ps.setObject(10, utc(at));
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    public List<CallResultRow> results(long reminderId) {
        return jdbc.query(
                "SELECT id, called_by, called_at, called_phone_number, result, reason, answered_by, note, supplementary"
                        + " FROM call_result WHERE call_reminder_id = ? ORDER BY id",
                CallReminderStore::result,
                reminderId);
    }

    /** Kết quả gọi của mọi lượt trong chuỗi, cho thông báo Quản lý viện khi hết người (FR-029a). */
    public List<CallResultRow> chainResults(long chainId) {
        return jdbc.query(
                "SELECT c.id, c.called_by, c.called_at, c.called_phone_number, c.result, c.reason, c.answered_by,"
                        + " c.note, c.supplementary FROM call_result c JOIN call_reminder r ON r.id = c.call_reminder_id"
                        + " WHERE COALESCE(r.call_chain_id, r.id) = ? ORDER BY c.called_at, c.id",
                CallReminderStore::result,
                chainId);
    }

    /** Lượt Chờ kết quả của chuỗi gọi. */
    public List<Long> awaitingInChain(long chainId) {
        return jdbc.queryForList(
                "SELECT r.id FROM call_reminder r JOIN call_reminder_state s ON s.call_reminder_id = r.id"
                        + " WHERE COALESCE(r.call_chain_id, r.id) = ? AND s.status = 'AWAITING_RESULT' ORDER BY r.id",
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
                "SELECT notification_id FROM call_reminder_notification WHERE call_reminder_id = ? ORDER BY id",
                Long.class,
                reminderId);
    }

    /** Mọi lượt của các chuỗi gọi có thông báo này, theo thứ tự tạo (tra cứu cho module nguồn, FR-031). */
    public List<Long> chainRemindersOf(long notificationId) {
        return jdbc.queryForList(
                "SELECT x.id FROM call_reminder x WHERE COALESCE(x.call_chain_id, x.id) IN (SELECT"
                        + " COALESCE(r.call_chain_id, r.id) FROM call_reminder r JOIN call_reminder_notification l"
                        + " ON l.call_reminder_id = r.id WHERE l.notification_id = ?) ORDER BY x.id",
                Long.class,
                notificationId);
    }

    /** Lượt có thông báo này. */
    public List<Long> remindersOf(long notificationId) {
        return jdbc.queryForList(
                "SELECT call_reminder_id FROM call_reminder_notification WHERE notification_id = ?",
                Long.class,
                notificationId);
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

    private static CallResultRow result(ResultSet rs, int i) throws SQLException {
        return new CallResultRow(
                rs.getLong(1),
                rs.getLong(2),
                instant(rs, 3),
                rs.getString(4),
                rs.getString(5),
                rs.getString(6),
                rs.getString(7),
                rs.getString(8),
                rs.getBoolean(9));
    }

    private static Instant instant(ResultSet rs, int column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class).toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
