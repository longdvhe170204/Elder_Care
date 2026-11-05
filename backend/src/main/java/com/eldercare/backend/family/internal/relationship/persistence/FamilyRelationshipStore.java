package com.eldercare.backend.family.internal.relationship.persistence;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Đọc, ghi {@code family_member}, {@code family_relationship} (V012_01); lệnh ghi chạy dưới khóa người cao tuổi. */
@Repository
public class FamilyRelationshipStore {

    private static final String SELECT = "SELECT id, family_member_id, resident_id, relationship, representative,"
            + " primary_contact, contact_order, view_health, view_costs, receive_urgent, register_visits,"
            + " pickup_allowed, request_services, status, started_at, ended_at FROM family_relationship";
    private static final String MEMBER_SELECT = "SELECT id, full_name, phone_number, id_document_type,"
            + " id_document_number, address, version FROM family_member";

    private final JdbcTemplate jdbc;

    FamilyRelationshipStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<FamilyMember> familyMember(long id) {
        return jdbc.query(MEMBER_SELECT + " WHERE id = ?", FamilyRelationshipStore::member, id).stream()
                .findFirst();
    }

    /** Tìm theo họ tên, số điện thoại, số giấy tờ (FR-001). */
    public List<FamilyMember> searchFamilyMembers(String q, int limit) {
        String like = "%" + q + "%";
        return jdbc.query(
                MEMBER_SELECT + " WHERE full_name LIKE ? OR phone_number LIKE ? OR id_document_number LIKE ?"
                        + " ORDER BY full_name, id LIMIT ?",
                FamilyRelationshipStore::member,
                like,
                like,
                like,
                limit);
    }

    public long insertFamilyMember(
            String fullName,
            String phoneNumber,
            String idDocumentType,
            String idDocumentNumber,
            String address,
            long createdBy,
            Instant at) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement s = c.prepareStatement(
                            "INSERT INTO family_member (full_name, phone_number, id_document_type, id_document_number,"
                                    + " address, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setString(1, fullName);
                    s.setString(2, phoneNumber);
                    s.setString(3, idDocumentType);
                    s.setString(4, idDocumentNumber);
                    s.setString(5, address);
                    s.setLong(6, createdBy);
                    s.setObject(7, utc(at));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Sửa thông tin liên hệ theo phiên bản (If-Match); trả số dòng đã sửa. */
    public int updateFamilyMember(
            long id,
            int version,
            String fullName,
            String phoneNumber,
            String idDocumentType,
            String idDocumentNumber,
            String address) {
        return jdbc.update(
                "UPDATE family_member SET full_name = ?, phone_number = ?, id_document_type = ?,"
                        + " id_document_number = ?, address = ?, version = version + 1 WHERE id = ? AND version = ?",
                fullName,
                phoneNumber,
                idDocumentType,
                idDocumentNumber,
                address,
                id,
                version);
    }

    public Optional<FamilyRelationship> relationship(long id) {
        return jdbc.query(SELECT + " WHERE id = ?", this::map, id).stream().findFirst();
    }

    /** Mọi quan hệ của người thân, kể cả đã kết thúc. */
    public List<FamilyRelationship> ofFamilyMember(long familyMemberId) {
        return jdbc.query(SELECT + " WHERE family_member_id = ? ORDER BY started_at, id", this::map, familyMemberId);
    }

    public int nextContactOrder(long residentId) {
        Integer max = jdbc.queryForObject(
                "SELECT MAX(contact_order) FROM family_relationship WHERE resident_id = ?", Integer.class, residentId);
        return max == null ? 1 : max + 1;
    }

    /** Quan hệ Hiệu lực mới; quyền ở {@code rights} bật, còn lại tắt (FR-008). */
    public long insertRelationship(
            long familyMemberId,
            long residentId,
            FamilyRelationshipType relationship,
            boolean representative,
            boolean primaryContact,
            int contactOrder,
            Set<FamilyRight> rights,
            long createdBy,
            Instant at) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement s = c.prepareStatement(
                            "INSERT INTO family_relationship (family_member_id, resident_id, relationship,"
                                    + " representative, primary_contact, contact_order, view_health, view_costs,"
                                    + " receive_urgent, register_visits, pickup_allowed, request_services, started_at,"
                                    + " created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, familyMemberId);
                    s.setLong(2, residentId);
                    s.setString(3, relationship.name());
                    s.setBoolean(4, representative);
                    s.setBoolean(5, primaryContact);
                    s.setInt(6, contactOrder);
                    s.setBoolean(7, rights.contains(FamilyRight.VIEW_HEALTH));
                    s.setBoolean(8, rights.contains(FamilyRight.VIEW_COSTS));
                    s.setBoolean(9, rights.contains(FamilyRight.RECEIVE_URGENT));
                    s.setBoolean(10, rights.contains(FamilyRight.REGISTER_VISITS));
                    s.setBoolean(11, rights.contains(FamilyRight.PICKUP_ALLOWED));
                    s.setBoolean(12, rights.contains(FamilyRight.REQUEST_SERVICES));
                    s.setObject(13, utc(at));
                    s.setLong(14, createdBy);
                    s.setObject(15, utc(at));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public void setPrimaryContact(long id, boolean value) {
        jdbc.update(
                "UPDATE family_relationship SET primary_contact = ?, version = version + 1 WHERE id = ?", value, id);
    }

    public void setRepresentative(long id, boolean value) {
        jdbc.update("UPDATE family_relationship SET representative = ?, version = version + 1 WHERE id = ?", value, id);
    }

    public void setRight(long id, FamilyRight right, boolean value) {
        jdbc.update(
                "UPDATE family_relationship SET " + column(right) + " = ?, version = version + 1 WHERE id = ?",
                value,
                id);
    }

    public void setContactOrder(long id, int contactOrder) {
        jdbc.update(
                "UPDATE family_relationship SET contact_order = ?, version = version + 1 WHERE id = ?",
                contactOrder,
                id);
    }

    public void end(long id, String reason, Instant at) {
        jdbc.update(
                "UPDATE family_relationship SET status = 'ENDED', ended_at = ?, end_reason = ?, version = version + 1"
                        + " WHERE id = ?",
                utc(at),
                reason,
                id);
    }

    private static FamilyMember member(ResultSet rs, int row) throws SQLException {
        return new FamilyMember(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("phone_number"),
                rs.getString("id_document_type"),
                rs.getString("id_document_number"),
                rs.getString("address"),
                rs.getInt("version"));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /** Quan hệ Hiệu lực của người thân. */
    public List<FamilyRelationship> activeOfFamilyMember(long familyMemberId) {
        return jdbc.query(
                SELECT + " WHERE family_member_id = ? AND status = 'ACTIVE' ORDER BY resident_id",
                this::map,
                familyMemberId);
    }

    /** Quan hệ Hiệu lực của người cao tuổi theo thứ tự liên hệ (FR-012). */
    public List<FamilyRelationship> activeOfResident(long residentId) {
        return jdbc.query(
                SELECT + " WHERE resident_id = ? AND status = 'ACTIVE' ORDER BY contact_order, id",
                this::map,
                residentId);
    }

    /** Mọi quan hệ của người cao tuổi, kể cả đã kết thúc, theo thứ tự liên hệ, để xét tại một thời điểm. */
    public List<FamilyRelationship> ofResident(long residentId) {
        return jdbc.query(SELECT + " WHERE resident_id = ? ORDER BY contact_order, id", this::map, residentId);
    }

    /** Mọi quan hệ của một cặp, kể cả đã kết thúc, để xét tại một thời điểm. */
    public List<FamilyRelationship> ofPair(long familyMemberId, long residentId) {
        return jdbc.query(
                SELECT + " WHERE family_member_id = ? AND resident_id = ? ORDER BY started_at, id",
                this::map,
                familyMemberId,
                residentId);
    }

    private FamilyRelationship map(ResultSet rs, int row) throws SQLException {
        Set<FamilyRight> rights = EnumSet.noneOf(FamilyRight.class);
        for (FamilyRight right : FamilyRight.values()) {
            if (rs.getBoolean(column(right))) {
                rights.add(right);
            }
        }
        return new FamilyRelationship(
                rs.getLong("id"),
                rs.getLong("family_member_id"),
                rs.getLong("resident_id"),
                FamilyRelationshipType.valueOf(rs.getString("relationship")),
                rs.getBoolean("representative"),
                rs.getBoolean("primary_contact"),
                rs.getInt("contact_order"),
                rights,
                "ACTIVE".equals(rs.getString("status")),
                instant(rs, "started_at"),
                instant(rs, "ended_at"));
    }

    private static String column(FamilyRight right) {
        return switch (right) {
            case VIEW_HEALTH -> "view_health";
            case VIEW_COSTS -> "view_costs";
            case REGISTER_VISITS -> "register_visits";
            case RECEIVE_URGENT -> "receive_urgent";
            case PICKUP_ALLOWED -> "pickup_allowed";
            case REQUEST_SERVICES -> "request_services";
        };
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
