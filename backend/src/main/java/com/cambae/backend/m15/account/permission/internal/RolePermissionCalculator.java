package com.cambae.backend.m15.account.permission.internal;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.PermissionKey;
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
 *   <li>hợp các vai trò; cùng khóa thì gộp phương án bằng {@link RoleGrants} (toàn viện thắng);
 *   <li>trừ thu hẹp theo tài khoản;
 *   <li>cộng quyền gán thêm (chỉ Duyệt kế hoạch chăm sóc, theo phân công như Điều dưỡng, FR-024).
 * </ol>
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

        // 1. Ma trận của từng vai trò trừ thu hẹp theo vai trò
        Map<String, Map<PermissionKey, Set<RoleGrant>>> byRole = new HashMap<>();
        jdbc.query(
                "SELECT m.role, m.function_code, m.operation, m.scope, m.footnote_codes FROM permission_matrix m"
                        + " JOIN role_assignment ra ON ra.role = m.role WHERE ra.account_id = ? AND "
                        + effectiveAt("ra"),
                rs -> {
                    byRole.computeIfAbsent(rs.getString(1), r -> new HashMap<>())
                            .put(
                                    key(rs.getString(2), rs.getString(3)),
                                    Set.of(new RoleGrant(
                                            PermissionScope.valueOf(rs.getString(4)),
                                            Set.of(json.readValue(rs.getString(5), String[].class)),
                                            GrantSource.ROLE)));
                },
                accountId,
                t,
                t);
        for (Restriction r : restrictions(
                "SELECT pr.role, pr.function_code, pr.operation, pr.kind FROM permission_restriction pr"
                        + " JOIN role_assignment ra ON ra.role = pr.role AND ra.account_id = ? AND " + effectiveAt("ra")
                        + " WHERE pr.applies_to = 'ROLE' AND pr.metric_group IS NULL AND pr.channel IS NULL AND "
                        + effectiveAt("pr"),
                accountId,
                t,
                t,
                t,
                t)) {
            Map<PermissionKey, Set<RoleGrant>> rolePermissions = byRole.get(r.role());
            if (rolePermissions != null) {
                r.applyTo(rolePermissions);
            }
        }

        // 2. Hợp các vai trò
        Map<PermissionKey, Set<RoleGrant>> result = new HashMap<>();
        for (Map<PermissionKey, Set<RoleGrant>> rolePermissions : byRole.values()) {
            rolePermissions.forEach((key, grants) -> {
                for (RoleGrant grant : grants) {
                    result.put(key, RoleGrants.add(result.getOrDefault(key, Set.of()), grant));
                }
            });
        }

        // 3. Trừ thu hẹp theo tài khoản
        for (Restriction r : restrictions(
                "SELECT NULL, function_code, operation, kind FROM permission_restriction WHERE applies_to = 'ACCOUNT'"
                        + " AND account_id = ? AND metric_group IS NULL AND channel IS NULL AND "
                        + effectiveAt("permission_restriction"),
                accountId,
                t,
                t)) {
            r.applyTo(result);
        }

        // 4. Cộng quyền gán thêm
        jdbc.query(
                "SELECT function_code, operation FROM additional_grant WHERE account_id = ? AND "
                        + effectiveAt("additional_grant"),
                rs -> {
                    PermissionKey key = key(rs.getString(1), rs.getString(2));
                    result.put(
                            key,
                            RoleGrants.add(
                                    result.getOrDefault(key, Set.of()),
                                    new RoleGrant(PermissionScope.ASSIGNED, Set.of(), GrantSource.ADDITIONAL_GRANT)));
                },
                accountId,
                t,
                t);
        return Map.copyOf(result);
    }

    private List<Restriction> restrictions(String sql, Object... args) {
        return jdbc.query(
                sql,
                (rs, i) -> new Restriction(
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

    /** Một dòng thu hẹp: {@code REMOVE_OPERATION} bỏ khóa, {@code LOWER_SCOPE} hạ phạm vi. */
    private record Restriction(String role, PermissionKey key, boolean lowerScope) {

        void applyTo(Map<PermissionKey, Set<RoleGrant>> permissions) {
            if (lowerScope) {
                permissions.computeIfPresent(key, (k, grants) -> RoleGrants.lower(grants));
            } else {
                permissions.remove(key);
            }
        }
    }

    private record Versions(long account, long roles) {}

    private record Cached(Versions versions, Map<PermissionKey, Set<RoleGrant>> permissions) {}
}
