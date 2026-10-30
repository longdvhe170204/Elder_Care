package com.eldercare.backend.resident.internal.consent.persistence;

import com.eldercare.backend.resident.api.dto.ConsenterType;
import com.eldercare.backend.resident.internal.consent.domain.Consent;
import com.eldercare.backend.resident.internal.consent.domain.ConsentStatus;
import com.eldercare.backend.resident.internal.consent.domain.CourtDecision;
import com.eldercare.backend.resident.internal.consent.domain.Withdrawal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Bản đồng ý ở bảng {@code consent} và {@code consent_family_member} (data-model mục 4). Cột sinh
 * {@code active_resident_id} có {@code UNIQUE} nên tối đa một bản Hiệu lực mỗi người cao tuổi (FR-013); trigger chỉ cho
 * đổi theo chiều Hiệu lực → Đã rút lại (FR-014, FR-016).
 */
@Repository
public class ConsentStore {

    private static final String SELECT = "SELECT id, resident_id, consenter_type, consenter_family_member_id,"
            + " court_decision_number, court_decision_date, court_decision_court, court_decision_file_id,"
            + " photo_consent, notification_consent, consented_at, evidence_file_id, status, withdrawn_at,"
            + " withdrawn_by, withdrawal_reason, withdrawal_evidence_file_id, superseded_by_id FROM consent";

    private final JdbcTemplate jdbc;

    ConsentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Thêm bản Hiệu lực cùng phạm vi người thân; trả id. */
    public long insert(
            long residentId,
            ConsenterType consenterType,
            Long consenterFamilyMemberId,
            CourtDecision courtDecision,
            Set<Long> familyMemberIds,
            boolean photoConsent,
            boolean notificationConsent,
            Instant consentedAt,
            long evidenceFileId,
            Instant createdAt,
            long createdBy) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO consent (resident_id, consenter_type, consenter_family_member_id,"
                                    + " court_decision_number, court_decision_date, court_decision_court,"
                                    + " court_decision_file_id, photo_consent, notification_consent, consented_at,"
                                    + " evidence_file_id, status, created_at, created_by)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, residentId);
                    statement.setString(2, consenterType.name());
                    statement.setObject(3, consenterFamilyMemberId);
                    statement.setObject(4, courtDecision == null ? null : courtDecision.number());
                    statement.setObject(5, courtDecision == null ? null : courtDecision.date());
                    statement.setObject(6, courtDecision == null ? null : courtDecision.court());
                    statement.setObject(7, courtDecision == null ? null : courtDecision.fileId());
                    statement.setBoolean(8, photoConsent);
                    statement.setBoolean(9, notificationConsent);
                    statement.setObject(10, utc(consentedAt));
                    statement.setLong(11, evidenceFileId);
                    statement.setObject(12, utc(createdAt));
                    statement.setLong(13, createdBy);
                    return statement;
                },
                keys);
        long id = keys.getKey().longValue();
        for (long familyMemberId : familyMemberIds) {
            jdbc.update(
                    "INSERT INTO consent_family_member (consent_id, family_member_id) VALUES (?, ?)",
                    id,
                    familyMemberId);
        }
        return id;
    }

    public Optional<Consent> find(long id) {
        return jdbc.query(SELECT + " WHERE id = ?", (rs, i) -> map(rs), id).stream()
                .findFirst();
    }

    public Optional<Consent> findActive(long residentId) {
        return jdbc
                .query(SELECT + " WHERE resident_id = ? AND status = 'ACTIVE'", (rs, i) -> map(rs), residentId)
                .stream()
                .findFirst();
    }

    /** Lịch sử bản đồng ý của người cao tuổi, mới nhất trước (FR-010, FR-016). */
    public List<Consent> listByResident(long residentId) {
        return jdbc.query(SELECT + " WHERE resident_id = ? ORDER BY id DESC", (rs, i) -> map(rs), residentId);
    }

    /** Bản Hiệu lực của người cao tuổi có người thân trong phạm vi xem sức khỏe không. */
    public boolean activeIncludes(long residentId, long familyMemberId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM consent c JOIN consent_family_member m ON m.consent_id = c.id"
                        + " WHERE c.resident_id = ? AND c.status = 'ACTIVE' AND m.family_member_id = ?",
                Integer.class,
                residentId,
                familyMemberId);
        return count != null && count > 0;
    }

    /** Như {@link #activeIncludes} nhưng tại thời điểm {@code at}: bản có hiệu lực từ lúc ghi tới lúc rút lại hoặc thay thế. */
    public boolean activeIncludesAt(long residentId, long familyMemberId, Instant at) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM consent c JOIN consent_family_member m ON m.consent_id = c.id"
                        + " WHERE c.resident_id = ? AND m.family_member_id = ? AND c.created_at <= ?"
                        + " AND (c.withdrawn_at IS NULL OR c.withdrawn_at > ?)",
                Integer.class,
                residentId,
                familyMemberId,
                utc(at),
                utc(at));
        return count != null && count > 0;
    }

    /** Chuyển bản Hiệu lực sang Đã rút lại bằng lệnh (lý do, bằng chứng); trả số dòng đổi. */
    public int withdraw(long id, long by, Instant at, String reason, long evidenceFileId) {
        return jdbc.update(
                "UPDATE consent SET status = 'WITHDRAWN', withdrawn_at = ?, withdrawn_by = ?, withdrawal_reason = ?,"
                        + " withdrawal_evidence_file_id = ? WHERE id = ? AND status = 'ACTIVE'",
                utc(at),
                by,
                reason,
                evidenceFileId,
                id);
    }

    /**
     * Chuyển bản Hiệu lực sang Đã rút lại vì sắp được thay thế (không lý do, không bằng chứng); {@link #supersededBy}
     * điền bản mới sau khi bản mới được thêm, vì chỉ một bản Hiệu lực tồn tại tại một thời điểm.
     */
    public int supersede(long id, long by, Instant at) {
        return jdbc.update(
                "UPDATE consent SET status = 'WITHDRAWN', withdrawn_at = ?, withdrawn_by = ? WHERE id = ? AND"
                        + " status = 'ACTIVE'",
                utc(at),
                by,
                id);
    }

    public void supersededBy(long id, long newId) {
        jdbc.update("UPDATE consent SET superseded_by_id = ? WHERE id = ?", newId, id);
    }

    private Consent map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        CourtDecision court = rs.getString("court_decision_number") == null
                ? null
                : new CourtDecision(
                        rs.getString("court_decision_number"),
                        rs.getObject("court_decision_date", LocalDate.class),
                        rs.getString("court_decision_court"),
                        rs.getObject("court_decision_file_id", Long.class));
        ConsentStatus status = ConsentStatus.valueOf(rs.getString("status"));
        Withdrawal withdrawal = status == ConsentStatus.ACTIVE
                ? null
                : new Withdrawal(
                        rs.getObject("withdrawn_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                        rs.getLong("withdrawn_by"),
                        rs.getString("withdrawal_reason"),
                        rs.getObject("withdrawal_evidence_file_id", Long.class),
                        rs.getObject("superseded_by_id", Long.class));
        return new Consent(
                id,
                rs.getLong("resident_id"),
                ConsenterType.valueOf(rs.getString("consenter_type")),
                rs.getObject("consenter_family_member_id", Long.class),
                court,
                new TreeSet<>(jdbc.queryForList(
                        "SELECT family_member_id FROM consent_family_member WHERE consent_id = ?", Long.class, id)),
                rs.getBoolean("photo_consent"),
                rs.getBoolean("notification_consent"),
                rs.getObject("consented_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getLong("evidence_file_id"),
                status,
                withdrawal);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
