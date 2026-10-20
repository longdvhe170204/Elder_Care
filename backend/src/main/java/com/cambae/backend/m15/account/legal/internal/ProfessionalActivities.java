package com.cambae.backend.m15.account.legal.internal;

import com.cambae.backend.m15.account.api.Function;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Đọc, ghi {@code professional_activity} và mục phạm vi hành nghề yêu cầu (002 T063) bằng JDBC như phần còn lại của M15. */
@Component
class ProfessionalActivities {

    private static final String COLUMNS =
            "id, code, name, function_code, required_license_type, requires_facility_scope, is_default, status, version";

    private final JdbcTemplate jdbc;

    ProfessionalActivities(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<ProfessionalActivity> all() {
        return read("SELECT " + COLUMNS + " FROM professional_activity ORDER BY id");
    }

    /** Mục đang hiệu lực của chức năng: thao tác chuyên môn phải thỏa mọi mục (FR-037). */
    List<ProfessionalActivity> activeFor(Function function) {
        return read(
                "SELECT " + COLUMNS + " FROM professional_activity WHERE function_code = ? AND status = 'ACTIVE'"
                        + " ORDER BY id",
                function.name());
    }

    Optional<ProfessionalActivity> byCode(String code) {
        return read("SELECT " + COLUMNS + " FROM professional_activity WHERE code = ?", code).stream()
                .findFirst();
    }

    Optional<ProfessionalActivity> lock(long id) {
        return read("SELECT " + COLUMNS + " FROM professional_activity WHERE id = ? FOR UPDATE", id).stream()
                .findFirst();
    }

    Optional<ProfessionalActivity> find(long id) {
        return read("SELECT " + COLUMNS + " FROM professional_activity WHERE id = ?", id).stream()
                .findFirst();
    }

    /** Mục mới, mã {@code PA-<id>} (mã kỹ thuật, cũng là mã phạm vi hoạt động của cơ sở khi mục cần). */
    long insert(String name, Function function, String licenseType, boolean requiresFacilityScope) {
        String placeholder = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO professional_activity (code, name, function_code, required_license_type,"
                        + " requires_facility_scope) VALUES (?, ?, ?, ?, ?)",
                placeholder,
                name,
                function.name(),
                licenseType,
                requiresFacilityScope);
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("UPDATE professional_activity SET code = ? WHERE id = ?", "PA-" + id, id);
        return id;
    }

    void update(long id, String name, Function function, String licenseType, boolean requiresFacilityScope) {
        jdbc.update(
                "UPDATE professional_activity SET name = ?, function_code = ?, required_license_type = ?,"
                        + " requires_facility_scope = ?, version = version + 1 WHERE id = ?",
                name,
                function.name(),
                licenseType,
                requiresFacilityScope,
                id);
    }

    void deactivate(long id) {
        jdbc.update("UPDATE professional_activity SET status = 'INACTIVE', version = version + 1 WHERE id = ?", id);
    }

    void replacePracticeScopes(long id, Set<Long> practiceScopes) {
        jdbc.update("DELETE FROM professional_activity_practice_scope WHERE activity_id = ?", id);
        for (long scope : practiceScopes) {
            jdbc.update(
                    "INSERT INTO professional_activity_practice_scope (activity_id, practice_scope_id) VALUES (?, ?)",
                    id,
                    scope);
        }
    }

    private List<ProfessionalActivity> read(String sql, Object... args) {
        record Row(
                long id,
                String code,
                String name,
                Function function,
                String licenseType,
                boolean facility,
                boolean isDefault,
                boolean active,
                long version) {}
        List<Row> rows = jdbc.query(
                sql,
                (rs, i) -> new Row(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        Function.valueOf(rs.getString("function_code")),
                        rs.getString("required_license_type"),
                        rs.getBoolean("requires_facility_scope"),
                        rs.getBoolean("is_default"),
                        "ACTIVE".equals(rs.getString("status")),
                        rs.getLong("version")),
                args);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Set<Long>> scopes = new HashMap<>();
        jdbc.query("SELECT activity_id, practice_scope_id FROM professional_activity_practice_scope", rs -> {
            scopes.computeIfAbsent(rs.getLong(1), k -> new HashSet<>()).add(rs.getLong(2));
        });
        return rows.stream()
                .map(r -> new ProfessionalActivity(
                        r.id(),
                        r.code(),
                        r.name(),
                        r.function(),
                        r.licenseType(),
                        Set.copyOf(scopes.getOrDefault(r.id(), Set.of())),
                        r.facility(),
                        r.isDefault(),
                        r.active(),
                        r.version()))
                .toList();
    }
}
