package com.eldercare.backend.family;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dữ liệu người thân, quan hệ lập thẳng bằng SQL và lịch sử như lệnh của US1 sẽ ghi (T011), cho test các bản cài đọc của
 * 012 trước khi có lệnh. Công khai cho test của module dùng bản cài thật thay bản giả (012 T038).
 */
public final class RelationshipFixtures {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final RelationshipHistoryStore history;
    private final Instant start;

    public RelationshipFixtures(
            JdbcTemplate jdbc, TransactionTemplate tx, RelationshipHistoryStore history, Instant start) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.history = history;
        this.start = start;
    }

    public long familyMember(String name, String phone) {
        return familyMember(name, phone, null, null);
    }

    public long familyMember(String name, String phone, String idDocumentType, String idDocumentNumber) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement s = c.prepareStatement(
                            "INSERT INTO family_member (full_name, phone_number, id_document_type,"
                                    + " id_document_number, created_by, created_at) VALUES (?, ?, ?, ?, 1, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setString(1, name);
                    s.setString(2, phone);
                    s.setString(3, idDocumentType);
                    s.setString(4, idDocumentNumber);
                    s.setObject(5, utc(start));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Quan hệ Hiệu lực từ {@code start}; vai trò, quyền ghi cả cột và lịch sử như lệnh của US1. */
    public long relate(
            long familyMemberId,
            long residentId,
            int order,
            boolean representative,
            boolean primaryContact,
            FamilyRight... rights) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        List<FamilyRight> enabled = List.of(rights);
        jdbc.update(
                c -> {
                    PreparedStatement s = c.prepareStatement(
                            "INSERT INTO family_relationship (family_member_id, resident_id, relationship,"
                                    + " representative, primary_contact, contact_order, view_health, view_costs,"
                                    + " register_visits, receive_urgent, pickup_allowed, request_services, started_at,"
                                    + " created_by, created_at) VALUES (?, ?, 'CHILD', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, familyMemberId);
                    s.setLong(2, residentId);
                    s.setBoolean(3, representative);
                    s.setBoolean(4, primaryContact);
                    s.setInt(5, order);
                    int i = 6;
                    for (FamilyRight right : FamilyRight.values()) {
                        s.setBoolean(i++, enabled.contains(right));
                    }
                    s.setObject(12, utc(start));
                    s.setObject(13, utc(start));
                    return s;
                },
                keys);
        long id = keys.getKey().longValue();
        tx.executeWithoutResult(s -> {
            if (representative) {
                history.record(id, RelationshipAttribute.REPRESENTATIVE, true, start, "CREATE", 1L, null);
            }
            for (FamilyRight right : enabled) {
                history.record(id, RelationshipAttribute.of(right), true, start, "CREATE", 1L, null);
            }
        });
        return id;
    }

    /** Đổi giá trị từ {@code at} trong lịch sử, như lệnh yêu cầu thay đổi quyền (US3). */
    public void record(long relationshipId, RelationshipAttribute attribute, boolean value, Instant at) {
        tx.executeWithoutResult(s -> history.record(relationshipId, attribute, value, at, "REQUEST", 1L, null));
    }

    public void end(long relationshipId, Instant at) {
        jdbc.update(
                "UPDATE family_relationship SET status = 'ENDED', ended_at = ?, end_reason = 'test' WHERE id = ?",
                utc(at),
                relationshipId);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
