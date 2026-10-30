package com.eldercare.backend.administration.account.internal.permission.persistence;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.internal.permission.domain.GrantSource;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionScope;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.Restriction;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.RestrictionKind;
import com.eldercare.backend.administration.account.internal.permission.domain.RoleGrant;
import com.eldercare.backend.administration.account.internal.permission.domain.RolePermissionRules.RestrictionRule;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đọc {@code permission_matrix}, {@code permission_assignment} cho phép tính quyền vai trò và tập quyền của tài khoản
 * (002 research A-05; FR-025, FR-026a, BR-M15-18). Mọi dòng lọc theo {@code effective_from <= t < effective_to} nên tính
 * được tại thời điểm quá khứ. Thu hẹp chi tiết của {@code DASHBOARD_REPORT} (có {@code metric_group} hoặc
 * {@code channel}, 016 FR-010a) không trả ở mức khóa.
 */
@Component
public class PermissionReads {

    private static final String ROLE_AT = "ra.kind = 'ROLE_ASSIGNMENT' AND " + effectiveAt("ra");

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    PermissionReads(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long accountPermissionVersion(long accountId) {
        return jdbc.queryForObject("SELECT permission_version FROM account WHERE id = ?", Long.class, accountId);
    }

    /** Vai trò có dòng gán chưa đóng (phần vai trò của khóa bộ đệm quyền). */
    public Set<String> openRoles(long accountId) {
        return Set.copyOf(jdbc.queryForList(
                "SELECT role FROM permission_assignment WHERE kind = 'ROLE_ASSIGNMENT' AND account_id = ?"
                        + " AND effective_to IS NULL",
                String.class,
                accountId));
    }

    /** Ô ma trận của từng vai trò gán tại {@code at}. */
    public Map<String, Map<PermissionKey, Set<RoleGrant>>> matrixOfAssignedRoles(long accountId, Instant at) {
        LocalDateTime t = utc(at);
        Map<String, Map<PermissionKey, Set<RoleGrant>>> byRole = new HashMap<>();
        jdbc.query(
                "SELECT m.role, m.function_code, m.operation, m.scope, m.footnote_codes FROM permission_matrix m"
                        + " JOIN permission_assignment ra ON ra.role = m.role WHERE ra.account_id = ? AND " + ROLE_AT,
                rs -> {
                    byRole.computeIfAbsent(rs.getString(1), r -> new HashMap<>())
                            .put(key(rs.getString(2), rs.getString(3)), grant(rs));
                },
                accountId,
                t,
                t);
        return byRole;
    }

    /** Ô ma trận của một vai trò. */
    public Map<PermissionKey, Set<RoleGrant>> matrixOfRole(String role) {
        Map<PermissionKey, Set<RoleGrant>> matrix = new HashMap<>();
        jdbc.query(
                "SELECT role, function_code, operation, scope, footnote_codes FROM permission_matrix WHERE role = ?",
                rs -> {
                    matrix.put(key(rs.getString(2), rs.getString(3)), grant(rs));
                },
                role);
        return matrix;
    }

    /** Thu hẹp theo vai trò của các vai trò gán tại {@code at}, mức khóa. */
    public List<RestrictionRule> roleRestrictionRules(long accountId, Instant at) {
        LocalDateTime t = utc(at);
        return rules(
                "SELECT pr.role, pr.function_code, pr.operation, pr.restriction_kind FROM permission_assignment pr"
                        + " JOIN permission_assignment ra ON ra.role = pr.role AND ra.account_id = ? AND " + ROLE_AT
                        + " WHERE pr.kind = 'RESTRICTION' AND pr.applies_to = 'ROLE' AND pr.metric_group IS NULL"
                        + " AND pr.channel IS NULL AND " + effectiveAt("pr"),
                accountId,
                t,
                t,
                t,
                t);
    }

    /** Thu hẹp theo tài khoản tại {@code at}, mức khóa. */
    public List<RestrictionRule> accountRestrictionRules(long accountId, Instant at) {
        LocalDateTime t = utc(at);
        return rules(
                "SELECT NULL, function_code, operation, restriction_kind FROM permission_assignment pr"
                        + " WHERE kind = 'RESTRICTION' AND applies_to = 'ACCOUNT' AND account_id = ?"
                        + " AND metric_group IS NULL AND channel IS NULL AND " + effectiveAt("pr"),
                accountId,
                t,
                t);
    }

    /** Thu hẹp theo vai trò của một vai trò tại {@code at}, mức khóa. */
    public List<RestrictionRule> restrictionRulesOfRole(String role, Instant at) {
        LocalDateTime t = utc(at);
        return rules(
                "SELECT role, function_code, operation, restriction_kind FROM permission_assignment pr"
                        + " WHERE kind = 'RESTRICTION' AND applies_to = 'ROLE' AND role = ? AND metric_group IS NULL"
                        + " AND channel IS NULL AND " + effectiveAt("pr"),
                role,
                t,
                t);
    }

    /** Quyền gán thêm tại {@code at}, chỉ khi tài khoản đang có vai trò Điều dưỡng tại {@code at} (FR-024). */
    public Set<PermissionKey> nurseGrants(long accountId, Instant at) {
        LocalDateTime t = utc(at);
        return new HashSet<>(jdbc.query(
                "SELECT g.function_code, g.operation FROM permission_assignment g WHERE g.kind = 'ADDITIONAL_GRANT'"
                        + " AND g.account_id = ? AND " + effectiveAt("g")
                        + " AND EXISTS (SELECT 1 FROM permission_assignment ra WHERE ra.account_id = g.account_id"
                        + " AND ra.role = 'AC-04' AND " + ROLE_AT + ")",
                (rs, i) -> key(rs.getString(1), rs.getString(2)),
                accountId,
                t,
                t,
                t,
                t));
    }

    /** Quyền gán thêm đang hiệu lực tại {@code at}, không xét vai trò (hiển thị, FR-026). */
    public List<PermissionKey> grants(long accountId, Instant at) {
        LocalDateTime t = utc(at);
        return jdbc.query(
                "SELECT function_code, operation FROM permission_assignment g WHERE kind = 'ADDITIONAL_GRANT'"
                        + " AND account_id = ? AND " + effectiveAt("g"),
                (rs, i) -> key(rs.getString(1), rs.getString(2)),
                accountId,
                t,
                t);
    }

    /** Thu hẹp theo vai trò của các vai trò gán tại {@code at}, kể cả thu hẹp chi tiết, theo thứ tự lập. */
    public List<Restriction> roleRestrictions(long accountId, Instant at) {
        LocalDateTime t = utc(at);
        return restrictions(
                "SELECT pr.id, pr.function_code, pr.operation, pr.restriction_kind, pr.effective_from, pr.reason"
                        + " FROM permission_assignment pr JOIN permission_assignment ra ON ra.role = pr.role"
                        + " AND ra.account_id = ? AND " + ROLE_AT + " WHERE pr.kind = 'RESTRICTION'"
                        + " AND pr.applies_to = 'ROLE' AND " + effectiveAt("pr") + " ORDER BY pr.id",
                accountId,
                t,
                t,
                t,
                t);
    }

    /** Thu hẹp theo tài khoản tại {@code at}, theo thứ tự lập. */
    public List<Restriction> accountRestrictions(long accountId, Instant at) {
        LocalDateTime t = utc(at);
        return restrictions(
                "SELECT id, function_code, operation, restriction_kind, effective_from, reason FROM permission_assignment pr"
                        + " WHERE kind = 'RESTRICTION' AND applies_to = 'ACCOUNT' AND account_id = ? AND "
                        + effectiveAt("pr") + " ORDER BY id",
                accountId,
                t,
                t);
    }

    /** Thu hẹp theo vai trò của một vai trò tại {@code at}, theo thứ tự lập. */
    public List<Restriction> restrictionsOfRole(String role, Instant at) {
        LocalDateTime t = utc(at);
        return restrictions(
                "SELECT id, function_code, operation, restriction_kind, effective_from, reason FROM permission_assignment pr"
                        + " WHERE kind = 'RESTRICTION' AND applies_to = 'ROLE' AND role = ? AND " + effectiveAt("pr")
                        + " ORDER BY id",
                role,
                t,
                t);
    }

    private Set<RoleGrant> grant(ResultSet rs) throws SQLException {
        return Set.of(new RoleGrant(
                PermissionScope.valueOf(rs.getString(4)),
                Set.of(json.readValue(rs.getString(5), String[].class)),
                GrantSource.ROLE));
    }

    private List<RestrictionRule> rules(String sql, Object... args) {
        return jdbc.query(
                sql,
                (rs, i) -> new RestrictionRule(
                        rs.getString(1), key(rs.getString(2), rs.getString(3)), "LOWER_SCOPE".equals(rs.getString(4))),
                args);
    }

    private List<Restriction> restrictions(String sql, Object... args) {
        return jdbc.query(
                sql,
                (rs, i) -> new Restriction(
                        rs.getLong(1),
                        rs.getString(2),
                        Operation.valueOf(rs.getString(3)),
                        RestrictionKind.valueOf(rs.getString(4)),
                        rs.getObject(5, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                        rs.getString(6)),
                args);
    }

    /** Dòng có hiệu lực tại t ({@code effective_from <= t < effective_to}); cần hai tham số t. */
    private static String effectiveAt(String alias) {
        return alias + ".effective_from <= ? AND (" + alias + ".effective_to IS NULL OR " + alias
                + ".effective_to > ?)";
    }

    private static PermissionKey key(String function, String operation) {
        return new PermissionKey(Function.valueOf(function), Operation.valueOf(operation));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
