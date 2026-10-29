package com.eldercare.backend.administration.account.internal.permission.service;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.internal.permission.domain.GrantSource;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionScope;
import com.eldercare.backend.administration.account.internal.permission.domain.RoleGrant;
import com.eldercare.backend.administration.account.internal.permission.domain.RolePermissionRules;
import com.eldercare.backend.administration.account.internal.permission.domain.RolePermissionRules.RestrictionRule;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Quyền vai trò của một tài khoản (002 research A-05; FR-025, FR-026a, BR-M15-18), theo thứ tự:
 *
 * <ol>
 *   <li>với từng vai trò đang gán: ma trận trừ thu hẹp theo vai trò;
 *   <li>hợp các vai trò; cùng khóa thì gộp phương án (toàn viện thắng);
 *   <li>trừ thu hẹp theo tài khoản;
 *   <li>cộng quyền gán thêm (chỉ Duyệt kế hoạch chăm sóc, theo phân công như Điều dưỡng, FR-024), chỉ khi tài khoản
 *       đang có vai trò Điều dưỡng.
 * </ol>
 *
 * <p>Thứ tự ở {@link RolePermissionRules}; lớp này đọc các bảng.
 *
 * <p>Mọi bảng lọc theo {@code effective_from <= t < effective_to} nên tính được tại thời điểm quá khứ. Thu hẹp chi tiết
 * của {@code DASHBOARD_REPORT} (có {@code metric_group} hoặc {@code channel}, 016 FR-010a) không trừ ở mức khóa. Điều
 * kiện chú thích, nhiệm vụ, phạm vi dữ liệu, pháp lý tính ở {@code PermissionApi} (research A-10).
 */
@Component
public class RolePermissionCalculator {

    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final JsonMapper json;
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    RolePermissionCalculator(JdbcTemplate jdbc, BusinessTime time, JsonMapper json) {
        this.jdbc = jdbc;
        this.time = time;
        this.json = json;
    }

    /**
     * Quyền hiện hành, có bộ đệm trong tiến trình theo {@code (account_id, account.permission_version, tổng
     * role.permission_version của các vai trò đang gán)}: lệnh đổi quyền tăng số phiên bản nên yêu cầu kế tiếp tính lại
     * (FR-027).
     */
    public Map<PermissionKey, Set<RoleGrant>> current(long accountId) {
        Versions versions = jdbc.queryForObject("""
                SELECT a.permission_version, COALESCE(SUM(r.permission_version), 0)
                FROM account a
                LEFT JOIN role_assignment ra ON ra.account_id = a.id AND ra.effective_to IS NULL
                LEFT JOIN role r ON r.code = ra.role
                WHERE a.id = ?
                GROUP BY a.id, a.permission_version
                """, (rs, i) -> new Versions(rs.getLong(1), rs.getLong(2)), accountId);
        Cached cached = cache.get(accountId);
        if (cached != null && cached.versions().equals(versions)) {
            return cached.permissions();
        }
        Map<PermissionKey, Set<RoleGrant>> permissions = at(accountId, time.now());
        cache.put(accountId, new Cached(versions, permissions));
        return permissions;
    }

    /** Quyền tại thời điểm {@code at}, không qua bộ đệm (FR-026a). */
    public Map<PermissionKey, Set<RoleGrant>> at(long accountId, Instant at) {
        LocalDateTime t = LocalDateTime.ofInstant(at, ZoneOffset.UTC);
        Map<String, Map<PermissionKey, Set<RoleGrant>>> byRole = new HashMap<>();
        jdbc.query(
                "SELECT m.role, m.function_code, m.operation, m.scope, m.footnote_codes FROM permission_matrix m"
                        + " JOIN role_assignment ra ON ra.role = m.role WHERE ra.account_id = ? AND "
                        + effectiveAt("ra"),
                rs -> {
                    byRole.computeIfAbsent(rs.getString(1), r -> new HashMap<>())
                            .put(key(rs.getString(2), rs.getString(3)), grant(rs));
                },
                accountId,
                t,
                t);
        List<RestrictionRule> roleRestrictions = restrictions(
                "SELECT pr.role, pr.function_code, pr.operation, pr.kind FROM permission_restriction pr"
                        + " JOIN role_assignment ra ON ra.role = pr.role AND ra.account_id = ? AND " + effectiveAt("ra")
                        + " WHERE pr.applies_to = 'ROLE' AND pr.metric_group IS NULL AND pr.channel IS NULL AND "
                        + effectiveAt("pr"),
                accountId,
                t,
                t,
                t,
                t);
        List<RestrictionRule> accountRestrictions = restrictions(
                "SELECT NULL, function_code, operation, kind FROM permission_restriction WHERE applies_to = 'ACCOUNT'"
                        + " AND account_id = ? AND metric_group IS NULL AND channel IS NULL AND "
                        + effectiveAt("permission_restriction"),
                accountId,
                t,
                t);
        // Quyền gán thêm chỉ tính khi tài khoản đang có vai trò Điều dưỡng tại t (FR-024 "tài khoản Điều dưỡng")
        Set<PermissionKey> grants = Set.copyOf(jdbc.query(
                "SELECT g.function_code, g.operation FROM additional_grant g WHERE g.account_id = ? AND "
                        + effectiveAt("g")
                        + " AND EXISTS (SELECT 1 FROM role_assignment ra WHERE ra.account_id = g.account_id"
                        + " AND ra.role = 'AC-04' AND " + effectiveAt("ra") + ")",
                (rs, i) -> key(rs.getString(1), rs.getString(2)),
                accountId,
                t,
                t,
                t,
                t));
        return RolePermissionRules.combine(byRole, roleRestrictions, accountRestrictions, grants);
    }

    /**
     * Quyền tối đa (ma trận) và quyền hiện hành của một vai trò sau thu hẹp theo vai trò, tại {@code at} (002 T076;
     * {@code GET /roles/{code}/permissions}, FR-018, FR-020).
     */
    public RolePermissions ofRole(String role, Instant at) {
        LocalDateTime t = LocalDateTime.ofInstant(at, ZoneOffset.UTC);
        Map<PermissionKey, Set<RoleGrant>> matrix = new HashMap<>();
        jdbc.query(
                "SELECT role, function_code, operation, scope, footnote_codes FROM permission_matrix WHERE role = ?",
                rs -> {
                    matrix.put(key(rs.getString(2), rs.getString(3)), grant(rs));
                },
                role);
        List<RestrictionRule> restrictions = restrictions(
                "SELECT role, function_code, operation, kind FROM permission_restriction WHERE applies_to = 'ROLE'"
                        + " AND role = ? AND metric_group IS NULL AND channel IS NULL AND "
                        + effectiveAt("permission_restriction"),
                role,
                t,
                t);
        return new RolePermissions(Map.copyOf(matrix), Map.copyOf(RolePermissionRules.ofRole(matrix, restrictions)));
    }

    /** Ma trận và quyền sau thu hẹp của một vai trò. */
    public record RolePermissions(
            Map<PermissionKey, Set<RoleGrant>> maximum, Map<PermissionKey, Set<RoleGrant>> current) {}

    private Set<RoleGrant> grant(java.sql.ResultSet rs) throws java.sql.SQLException {
        return Set.of(new RoleGrant(
                PermissionScope.valueOf(rs.getString(4)),
                Set.of(json.readValue(rs.getString(5), String[].class)),
                GrantSource.ROLE));
    }

    private List<RestrictionRule> restrictions(String sql, Object... args) {
        return jdbc.query(
                sql,
                (rs, i) -> new RestrictionRule(
                        rs.getString(1), key(rs.getString(2), rs.getString(3)), "LOWER_SCOPE".equals(rs.getString(4))),
                args);
    }

    /** Dòng có hiệu lực tại t ({@code effective_from <= t < effective_to}); cần hai tham số t. */
    private static String effectiveAt(String table) {
        return table + ".effective_from <= ? AND (" + table + ".effective_to IS NULL OR " + table
                + ".effective_to > ?)";
    }

    private static PermissionKey key(String function, String operation) {
        return new PermissionKey(Function.valueOf(function), Operation.valueOf(operation));
    }

    private record Versions(long account, long roles) {}

    private record Cached(Versions versions, Map<PermissionKey, Set<RoleGrant>> permissions) {}
}
