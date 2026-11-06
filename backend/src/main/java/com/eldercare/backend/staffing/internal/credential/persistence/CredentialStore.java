package com.eldercare.backend.staffing.internal.credential.persistence;

import com.eldercare.backend.staffing.internal.credential.domain.*;
import com.eldercare.backend.staffing.internal.persistence.StaffingCatalogStore;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** T050: chứng chỉ giữ id; danh mục và phạm vi trả id logic, JSON/FK lưu id vật lý. */
@Repository
public class CredentialStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final StaffingCatalogStore catalogs;

    public CredentialStore(JdbcTemplate jdbc, JsonMapper json, StaffingCatalogStore catalogs) {
        this.jdbc = jdbc;
        this.json = json;
        this.catalogs = catalogs;
    }

    /** Chỉ lệnh đính chính có nhật ký sửa mô tả; trả biến phiên trên cùng connection kể cả khi lỗi. */
    private void guardedUpdate(String sql, Object... args) {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) c -> {
            try (var control = c.createStatement()) {
                control.execute("SET @correct_credential=1");
                try (var p = c.prepareStatement(sql)) {
                    for (int n = 0; n < args.length; n++) p.setObject(n + 1, args[n]);
                    p.executeUpdate();
                } finally {
                    control.execute("SET @correct_credential=0");
                }
            }
            return null;
        });
    }

    public void replaces(long id, long oldId) {
        guardedUpdate("UPDATE credential SET replaces_id=? WHERE id=?", oldId, id);
    }

    public void correct(long id, CredentialInput input) {
        guardedUpdate(
                "UPDATE credential SET credential_type_id=?,credential_number=?,issuing_authority=?,issue_date=?,expiry_date=?,scan_file_id=?,practice_scope_ids=?,invalidity_applied_at=NULL WHERE id=?",
                catalogs.credentialTypeRow(input.credentialTypeId()).id(),
                input.credentialNumber(),
                input.issuingAuthority(),
                input.issueDate(),
                input.expiryDate(),
                input.scanFileId(),
                scopeJson(input.practiceScopes()),
                id);
    }

    public List<CredentialType> types() {
        return catalogs.credentialTypes();
    }

    public Optional<CredentialType> type(long id) {
        return catalogs.credentialType(id);
    }

    public long addType(
            String name, String group, boolean expiry, Set<String> roles, boolean scopes, boolean active, Instant at) {
        return catalogs.addCredentialType(name, group, expiry, roles, scopes, active, at);
    }

    private static final String SELECT =
            "SELECT c.*,EXISTS(SELECT 1 FROM credential n WHERE n.replaces_id=c.id) replaced FROM credential c";

    public List<Credential> staffCredentials(long staffId) {
        return jdbc.query(SELECT + " WHERE c.staff_id=? ORDER BY c.id", this::map, staffId);
    }

    public List<Credential> all() {
        return jdbc.query(SELECT + " ORDER BY c.id", this::map);
    }

    public Optional<Credential> find(long id) {
        return jdbc.query(SELECT + " WHERE c.id=?", this::map, id).stream().findFirst();
    }

    public long insert(long staffId, CredentialInput input, Long actor, Instant at) {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(
                            "INSERT INTO credential(staff_id,credential_type_id,credential_number,issuing_authority,issue_date,expiry_date,scan_file_id,created_at,created_by,practice_scope_ids) VALUES (?,?,?,?,?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, staffId);
                    ps.setLong(
                            2,
                            catalogs.credentialTypeRow(input.credentialTypeId()).id());
                    ps.setString(3, input.credentialNumber());
                    ps.setString(4, input.issuingAuthority());
                    ps.setObject(5, input.issueDate());
                    ps.setObject(6, input.expiryDate());
                    ps.setObject(7, input.scanFileId());
                    ps.setObject(8, LocalDateTime.ofInstant(at, ZoneOffset.UTC));
                    ps.setObject(9, actor);
                    ps.setString(10, scopeJson(input.practiceScopes()));
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    private String scopeJson(Set<String> codes) {
        return json.writeValueAsString(codes.stream()
                .map(catalogs::practiceScopePhysicalId)
                .distinct()
                .sorted()
                .toList());
    }

    private Credential map(ResultSet r, int row) throws SQLException {
        var type = catalogs.credentialTypePhysical(r.getLong("credential_type_id"));
        var physicalScopes = json.readValue(r.getString("practice_scope_ids"), Long[].class);
        var scopeIds = new LinkedHashSet<Long>();
        var scopeCodes = new LinkedHashSet<String>();
        for (long physical : physicalScopes) {
            var scope = catalogs.practiceScopePhysical(physical);
            scopeIds.add(scope.id());
            scopeCodes.add(scope.code());
        }
        return new Credential(
                r.getLong("id"),
                r.getLong("staff_id"),
                type.id(),
                type.code(),
                type.name(),
                type.group(),
                r.getString("credential_number"),
                r.getString("issuing_authority"),
                Set.copyOf(scopeCodes),
                Set.copyOf(scopeIds),
                r.getObject("issue_date", LocalDate.class),
                r.getObject("expiry_date", LocalDate.class),
                r.getObject("scan_file_id", Long.class),
                r.getObject("replaces_id", Long.class),
                r.getObject("revoked_from", LocalDate.class),
                r.getBoolean("replaced"));
    }

    public List<Long> awaitingInvalidity() {
        return jdbc.queryForList("SELECT id FROM credential WHERE invalidity_applied_at IS NULL", Long.class);
    }

    public void markInvalidityApplied(long id, Instant at) {
        jdbc.update(
                "UPDATE credential SET invalidity_applied_at=? WHERE id=?",
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                id);
    }

    public int claimExpiryWarning(long id, Instant at) {
        return jdbc.update(
                "UPDATE credential SET expiry_warning_at=? WHERE id=? AND expiry_warning_at IS NULL",
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                id);
    }

    public int activePracticeScopeCount(String code) {
        return (int) catalogs.practiceScopes().stream()
                .filter(p -> p.active() && p.code().equalsIgnoreCase(code))
                .count();
    }

    public void resetExpiryWarning(long id) {
        jdbc.update("UPDATE credential SET expiry_warning_at=NULL WHERE id=?", id);
    }

    public void revoke(long id, LocalDate from, String reason) {
        jdbc.update(
                "UPDATE credential SET revoked_from=?,revocation_reason=?,invalidity_applied_at=NULL WHERE id=?",
                from,
                reason,
                id);
    }

    public void resetInvalidity(long id) {
        jdbc.update("UPDATE credential SET invalidity_applied_at=NULL WHERE id=?", id);
    }

    public List<PracticeScope> practiceScopes() {
        return catalogs.practiceScopes();
    }

    public Optional<PracticeScope> practiceScope(long id) {
        return catalogs.practiceScope(id);
    }
}
