package com.eldercare.backend.family.internal.relationship.persistence;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc {@code family_member}, {@code family_relationship} (V012_01). */
@Repository
public class FamilyRelationshipStore {

    private static final String SELECT = "SELECT id, family_member_id, resident_id, relationship, representative,"
            + " primary_contact, contact_order, view_health, view_costs, receive_urgent, register_visits,"
            + " pickup_allowed, request_services, status, started_at, ended_at FROM family_relationship";

    private final JdbcTemplate jdbc;

    FamilyRelationshipStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<FamilyMember> familyMember(long id) {
        return jdbc
                .query(
                        "SELECT id, full_name, phone_number, id_document_type, id_document_number, address, version"
                                + " FROM family_member WHERE id = ?",
                        (rs, i) -> new FamilyMember(
                                rs.getLong("id"),
                                rs.getString("full_name"),
                                rs.getString("phone_number"),
                                rs.getString("id_document_type"),
                                rs.getString("id_document_number"),
                                rs.getString("address"),
                                rs.getInt("version")),
                        id)
                .stream()
                .findFirst();
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
