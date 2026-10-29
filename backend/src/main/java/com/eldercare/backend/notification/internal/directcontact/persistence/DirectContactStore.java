package com.eldercare.backend.notification.internal.directcontact.persistence;

import com.eldercare.backend.notification.internal.directcontact.domain.DirectContact;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Danh sách "cần liên hệ trực tiếp" và kết quả liên hệ (FR-021; data-model mục 9). */
@Repository
public class DirectContactStore {

    /** Thông báo tới người thân không tới được qua kênh nào. */
    public record Unreached(long notificationId, long familyMemberId, Long residentId, String shortContent) {}

    private static final String ROW = "SELECT d.id, d.notification_id, d.family_member_id, d.full_name, d.relationship,"
            + " d.phone_number, d.short_content, d.status, d.created_at, r.resident_id, r.floor_area_id"
            + " FROM direct_contact d JOIN notification n ON n.id = d.notification_id"
            + " JOIN notification_request r ON r.id = n.request_id";

    private final JdbcTemplate jdbc;

    DirectContactStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Thông báo không khẩn tới người thân, đã gửi đi từ {@code since}: Trung bình, hoặc người thân gắn bản ghi nguồn (nhóm
     * (d)); không có lần gửi nào tới được, không còn lần gửi chờ kết quả hay chờ gửi lại; chưa có trong danh sách.
     */
    public List<Unreached> unreached(Instant since, int maxAttemptNo, int limit) {
        return jdbc.query(
                "SELECT n.id, n.recipient_id, r.resident_id, r.short_content FROM notification n"
                        + " JOIN notification_request r ON r.id = n.request_id"
                        + " JOIN notification_state s ON s.notification_id = n.id"
                        + " WHERE n.recipient_type = 'FAMILY_MEMBER' AND n.level <> 'URGENT'"
                        + " AND (n.level = 'MEDIUM' OR JSON_SEARCH(n.basis, 'one', 'FamilyMember:%') IS NOT NULL)"
                        + " AND s.sent_at IS NOT NULL AND n.created_at >= ?"
                        + " AND NOT EXISTS (SELECT 1 FROM direct_contact d WHERE d.notification_id = n.id)"
                        + " AND NOT EXISTS (SELECT 1 FROM delivery_attempt a"
                        + " LEFT JOIN delivery_result x ON x.attempt_id = a.id WHERE a.notification_id = n.id"
                        + " AND (x.id IS NULL OR x.result IN ('SENT', 'BATCHED') OR (x.result = 'FAILED'"
                        + " AND x.reason LIKE 'TEMPORARY%' AND a.attempt_no <= ? AND a.attempt_no = (SELECT"
                        + " MAX(y.attempt_no) FROM delivery_attempt y WHERE y.notification_id = a.notification_id"
                        + " AND y.channel = a.channel))))"
                        + " ORDER BY n.id LIMIT ?",
                (rs, i) -> new Unreached(rs.getLong(1), rs.getLong(2), rs.getObject(3, Long.class), rs.getString(4)),
                utc(since),
                maxAttemptNo,
                limit);
    }

    /** Thêm mục Chờ liên hệ; {@code false} khi thông báo đã có trong danh sách. */
    public boolean insert(
            long notificationId,
            long familyMemberId,
            String fullName,
            String phoneNumber,
            String shortContent,
            Instant at) {
        try {
            jdbc.update(
                    "INSERT INTO direct_contact (notification_id, family_member_id, full_name, phone_number,"
                            + " short_content, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    notificationId,
                    familyMemberId,
                    fullName,
                    phoneNumber,
                    shortContent,
                    utc(at));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public List<DirectContact> list(Optional<String> status, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(ROW);
        status.ifPresent(s -> {
            sql.append(" WHERE d.status = ?");
            args.add(s);
        });
        sql.append(" ORDER BY d.created_at DESC, d.id DESC LIMIT ?");
        args.add(limit);
        return jdbc.query(sql.toString(), (rs, i) -> row(rs), args.toArray());
    }

    public Optional<DirectContact> lock(long id) {
        return jdbc.query(ROW + " WHERE d.id = ? FOR UPDATE OF d", (rs, i) -> row(rs), id).stream()
                .findFirst();
    }

    public void insertResult(
            long id, long contactedBy, Instant occurredAt, String result, String reason, String note, Instant at) {
        jdbc.update(
                "INSERT INTO direct_contact_result (direct_contact_id, contacted_by, occurred_at, result, reason,"
                        + " note, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                contactedBy,
                utc(occurredAt),
                result,
                reason,
                note,
                utc(at));
        jdbc.update("UPDATE direct_contact SET status = ?, version = version + 1 WHERE id = ?", result, id);
    }

    /** Số mục còn Chờ liên hệ; đánh dấu đã nhắc (FR-021). */
    public int markReminded(Instant at) {
        return jdbc.update("UPDATE direct_contact SET reminded_at = ? WHERE status = 'PENDING'", utc(at));
    }

    private static DirectContact row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DirectContact(
                rs.getLong(1),
                rs.getLong(2),
                rs.getObject(3, Long.class),
                rs.getString(4),
                rs.getString(5),
                rs.getString(6),
                rs.getString(7),
                rs.getString(8),
                rs.getObject(9, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getObject(10, Long.class),
                rs.getObject(11, Long.class));
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
