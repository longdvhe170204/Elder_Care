package com.eldercare.backend.notification.internal.directcontact.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.notification.internal.directcontact.domain.DirectContact;
import com.eldercare.backend.notification.internal.directcontact.domain.DirectContactContent;
import com.eldercare.backend.notification.internal.directcontact.domain.DirectContactResultContent;
import com.eldercare.backend.notification.internal.directcontact.domain.RecordedContactResult;
import com.eldercare.backend.notification.internal.directcontact.domain.Unreached;
import com.eldercare.backend.notification.internal.domain.NotificationEventContent;
import com.eldercare.backend.notification.internal.persistence.M13SharedRows;
import com.eldercare.backend.notification.internal.persistence.M13Sql;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Danh sách cần liên hệ trực tiếp ở {@code simple_record} loại {@code M13.DIRECT_CONTACT} và kết quả liên hệ ở
 * {@code record_event} (009 T043, data-model mục 0.1; FR-021): một mục mỗi thông báo, một kết quả mỗi mục; mục khóa khi
 * rời Chờ liên hệ.
 */
@Repository
public class DirectContactStore {

    private static final String ROW =
            "SELECT d.id, d.entity_id, JSON_VALUE(d.content, '$.familyMemberId' RETURNING SIGNED),"
                    + " JSON_VALUE(d.content, '$.fullName'), JSON_VALUE(d.content, '$.relationship'),"
                    + " JSON_VALUE(d.content, '$.phoneNumber'), JSON_VALUE(d.content, '$.shortContent'), d.status, d.created_at,"
                    + " r.resident_id, r.floor_area_id FROM simple_record d JOIN notification n ON n.id = d.entity_id"
                    + " JOIN notification_request r ON r.id = n.request_id WHERE d.kind = 'M13.DIRECT_CONTACT'";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final SimpleRecordApi records;
    private final M13SharedRows shared;

    DirectContactStore(JdbcTemplate jdbc, JsonMapper json, SimpleRecordApi records, M13SharedRows shared) {
        this.jdbc = jdbc;
        this.json = json;
        this.records = records;
        this.shared = shared;
    }

    /**
     * Thông báo không khẩn tới người thân, đã gửi đi từ {@code since}: Trung bình, hoặc người thân gắn bản ghi nguồn (nhóm
     * (d)); không có lần gửi nào tới được, không còn lần gửi chờ kết quả hay chờ gửi lại; chưa có trong danh sách.
     */
    public List<Unreached> unreached(Instant since, int maxAttemptNo, int limit) {
        return jdbc.query(
                "SELECT n.id, n.recipient_id, r.resident_id, r.short_content FROM notification n"
                        + " JOIN notification_request r ON r.id = n.request_id"
                        + " WHERE n.recipient_type = 'FAMILY_MEMBER' AND n.level <> 'URGENT'"
                        + " AND (n.level = 'MEDIUM' OR JSON_SEARCH(n.basis, 'one', 'FamilyMember:%') IS NOT NULL)"
                        + " AND n.sent_at IS NOT NULL AND n.created_at >= ?"
                        + " AND NOT EXISTS (SELECT 1 FROM simple_record d WHERE d.unique_key = CONCAT('M13.DC:', n.id))"
                        + " AND NOT EXISTS (SELECT 1 FROM record_event a LEFT JOIN record_event x ON "
                        + M13Sql.resultOf("x", "a") + " WHERE " + M13Sql.attempts("a") + " AND a.entity_id = n.id"
                        + " AND (x.id IS NULL OR x.m13_result IN ('SENT', 'BATCHED') OR (x.m13_result = 'FAILED'"
                        + " AND JSON_VALUE(x.content, '$.reason') LIKE 'TEMPORARY%' AND " + M13Sql.attemptNo("a")
                        + " <= ? AND " + M13Sql.latestOfChannel("a") + ")))"
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
            shared.createRecord(
                    DirectContactContent.KIND,
                    new EntityRef(NotificationEventContent.NOTIFICATION, notificationId),
                    DirectContactContent.PENDING,
                    new DirectContactContent(familyMemberId, fullName, null, phoneNumber, shortContent, null),
                    DirectContactContent.uniqueKey(notificationId));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public List<DirectContact> list(Optional<String> status, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(ROW);
        status.ifPresent(s -> {
            sql.append(" AND d.status = ?");
            args.add(s);
        });
        sql.append(" ORDER BY d.created_at DESC, d.id DESC LIMIT ?");
        args.add(limit);
        return jdbc.query(sql.toString(), (rs, i) -> row(rs), args.toArray());
    }

    public Optional<DirectContact> lock(long id) {
        return jdbc.query(ROW + " AND d.id = ? FOR UPDATE OF d", (rs, i) -> row(rs), id).stream()
                .findFirst();
    }

    /** Ghi kết quả liên hệ và chuyển mục sang trạng thái cuối (khóa dòng). */
    public void insertResult(
            long id, long contactedBy, Instant occurredAt, String result, String reason, String note, Instant at) {
        shared.addEvent(
                DirectContactResultContent.KIND,
                DirectContactContent.KIND.code(),
                id,
                new DirectContactResultContent(occurredAt, result, reason, note),
                contactedBy,
                at,
                DirectContactResultContent.uniqueKey(id));
        SimpleRecord<DirectContactContent> d = records.require(DirectContactContent.KIND, id);
        records.change(
                DirectContactContent.KIND,
                id,
                d.version(),
                new RecordChange<>(result, d.content(), null, d.uniqueKey(), true));
    }

    /** Số mục còn Chờ liên hệ; đánh dấu đã nhắc (FR-021). */
    @Transactional
    public int markReminded(Instant at) {
        List<SimpleRecord<DirectContactContent>> pending =
                records.listByStatus(DirectContactContent.KIND, Set.of(DirectContactContent.PENDING));
        for (SimpleRecord<DirectContactContent> d : pending) {
            records.change(
                    DirectContactContent.KIND,
                    d.id(),
                    d.version(),
                    new RecordChange<>(d.status(), d.content().reminded(at), null, d.uniqueKey(), false));
        }
        return pending.size();
    }

    /** Kết quả liên hệ {@code id} (đích của đính chính {@code M13.DIRECT_CONTACT_RESULT}) và người liên hệ. */
    public Optional<RecordedContactResult> result(long id) {
        return jdbc
                .query(
                        "SELECT x.actor_id, x.content, q.resident_id FROM record_event x JOIN simple_record d ON d.id = x.entity_id"
                                + " JOIN notification n ON n.id = d.entity_id JOIN notification_request q ON q.id = n.request_id"
                                + " WHERE x.id = ? AND x.kind = 'M13.DIRECT_CONTACT_RESULT'",
                        (rs, i) -> new RecordedContactResult(
                                rs.getLong(1),
                                json.readValue(rs.getString(2), DirectContactResultContent.class),
                                rs.getObject(3, Long.class)),
                        id)
                .stream()
                .findFirst();
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
