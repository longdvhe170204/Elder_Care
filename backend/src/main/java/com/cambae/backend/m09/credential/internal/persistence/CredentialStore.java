package com.cambae.backend.m09.credential.internal.persistence;

import com.cambae.backend.m09.credential.internal.domain.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Kho dữ liệu chứng chỉ của M09; không truy cập dữ liệu module khác. */
@Repository
public class CredentialStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CredentialStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Chỉ lệnh có nhật ký được sửa mô tả; luôn trả biến phiên kể cả khi lỗi, trên cùng connection. */
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
                "UPDATE credential SET credential_type_id=?,credential_number=?,issuing_authority=?,issue_date=?,expiry_date=?,scan_file_id=?,invalidity_applied_at=NULL WHERE id=?",
                input.credentialTypeId(),
                input.credentialNumber(),
                input.issuingAuthority(),
                input.issueDate(),
                input.expiryDate(),
                input.scanFileId(),
                id);
        jdbc.update("DELETE FROM credential_practice_scope WHERE credential_id=?", id);
        for (String code : input.practiceScopes())
            jdbc.update(
                    "INSERT INTO credential_practice_scope(credential_id,practice_scope_id) SELECT ?,id FROM practice_scope WHERE code=?",
                    id,
                    code);
    }

    public List<CredentialType> types() {
        return jdbc.query(
                "SELECT * FROM credential_type ORDER BY id",
                (r, i) -> new CredentialType(
                        r.getLong("id"),
                        r.getString("code"),
                        r.getString("name"),
                        r.getString("credential_group"),
                        r.getBoolean("has_expiry"),
                        Set.of(json.readValue(r.getString("required_for_roles"), String[].class)),
                        r.getBoolean("has_practice_scopes"),
                        "ACTIVE".equals(r.getString("status"))));
    }

    public Optional<CredentialType> type(long id) {
        return types().stream().filter(t -> t.id() == id).findFirst();
    }

    public long addType(
            String name,
            String group,
            boolean hasExpiry,
            Set<String> roles,
            boolean scopes,
            boolean active,
            Instant at) {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(
                            "INSERT INTO credential_type(code,name,credential_group,has_expiry,required_for_roles,has_practice_scopes,status,created_at) VALUES(?,?,?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, "CT-" + UUID.randomUUID());
                    ps.setString(2, name.strip());
                    ps.setString(3, group);
                    ps.setBoolean(4, hasExpiry);
                    ps.setString(5, json.writeValueAsString(roles));
                    ps.setBoolean(6, scopes);
                    ps.setString(7, active ? "ACTIVE" : "INACTIVE");
                    ps.setObject(8, LocalDateTime.ofInstant(at, ZoneOffset.UTC));
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    private static final String SELECT =
            "SELECT c.*, t.code type_code,t.name type_name,t.credential_group,EXISTS(SELECT 1 FROM credential n WHERE n.replaces_id=c.id) replaced FROM credential c JOIN credential_type t ON t.id=c.credential_type_id";

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
                            "INSERT INTO credential(staff_id,credential_type_id,credential_number,issuing_authority,issue_date,expiry_date,scan_file_id,created_at,created_by) VALUES(?,?,?,?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, staffId);
                    ps.setLong(2, input.credentialTypeId());
                    ps.setString(3, input.credentialNumber());
                    ps.setString(4, input.issuingAuthority());
                    ps.setObject(5, input.issueDate());
                    ps.setObject(6, input.expiryDate());
                    ps.setObject(7, input.scanFileId());
                    ps.setObject(8, LocalDateTime.ofInstant(at, ZoneOffset.UTC));
                    ps.setObject(9, actor);
                    return ps;
                },
                key);
        long id = key.getKey().longValue();
        for (String code : input.practiceScopes())
            jdbc.update(
                    "INSERT INTO credential_practice_scope(credential_id,practice_scope_id) SELECT ?,id FROM practice_scope WHERE code=?",
                    id,
                    code);
        return id;
    }

    private Credential map(ResultSet r, int i) throws SQLException {
        long id = r.getLong("id");
        var scopeIds = new LinkedHashSet<>(jdbc.queryForList(
                "SELECT practice_scope_id FROM credential_practice_scope WHERE credential_id=?", Long.class, id));
        var scopeCodes = new LinkedHashSet<>(jdbc.queryForList(
                "SELECT p.code FROM credential_practice_scope s JOIN practice_scope p ON p.id=s.practice_scope_id WHERE s.credential_id=? ORDER BY p.id",
                String.class,
                id));
        return new Credential(
                id,
                r.getLong("staff_id"),
                r.getLong("credential_type_id"),
                r.getString("type_code"),
                r.getString("type_name"),
                r.getString("credential_group"),
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
}
