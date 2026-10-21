package com.cambae.backend.m15.account.internal.permission.persistence;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionScope;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.RestrictionKind;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.RoleInfo;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

/**
 * Bảng {@code role}, {@code permission_matrix}, {@code permission_restriction}, {@code additional_grant} cho lệnh đổi
 * quyền (002 T075; research A-05): thay đổi là đóng dòng cũ ({@code effective_to}) hoặc thêm dòng mới, không xóa.
 */
@Component
public class PermissionChanges {

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    PermissionChanges(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    /** Dòng thu hẹp, theo vai trò ({@code role}) hoặc theo tài khoản ({@code accountId}). */
    public record RestrictionRow(
            long id,
            String role,
            Long accountId,
            PermissionKey key,
            RestrictionKind kind,
            String metricGroup,
            String channel) {}

    public List<RoleInfo> roles() {
        return jdbc.query(
                "SELECT code, name, account_type, execution_scope FROM role ORDER BY code",
                (rs, i) -> new RoleInfo(
                        rs.getString(1), rs.getString(2), rs.getString(3), PermissionScope.valueOf(rs.getString(4))));
    }

    public Optional<Integer> roleVersion(String role) {
        return jdbc.queryForList("SELECT permission_version FROM role WHERE code = ?", Integer.class, role).stream()
                .findFirst();
    }

    /**
     * Tăng {@code role.permission_version} khi bằng {@code expected} (so và tăng trong một câu lệnh, giữ khóa dòng tới
     * hết transaction, BR-M15-17); trả {@code false} khi phiên bản đã khác.
     */
    public boolean bumpRoleVersion(String role, long expected) {
        return jdbc.update(
                        "UPDATE role SET permission_version = permission_version + 1 WHERE code = ?"
                                + " AND permission_version = ?",
                        role,
                        expected)
                == 1;
    }

    /** Phạm vi của ô ma trận của các vai trò đã cho cho một khóa; rỗng khi không vai trò nào có ô này (FR-025). */
    public Set<PermissionScope> matrixScopes(Set<String> roles, PermissionKey key) {
        if (roles.isEmpty()) {
            return Set.of();
        }
        String placeholders = roles.stream().map(r -> "?").collect(Collectors.joining(", "));
        Object[] args = new Object[roles.size() + 2];
        int i = 0;
        for (String role : roles) {
            args[i++] = role;
        }
        args[i++] = key.function().name();
        args[i] = key.operation().name();
        return Set.copyOf(jdbc.query(
                "SELECT scope FROM permission_matrix WHERE role IN (" + placeholders
                        + ") AND function_code = ? AND operation = ?",
                (rs, n) -> PermissionScope.valueOf(rs.getString(1)),
                args));
    }

    public List<RestrictionRow> activeRestrictions(String role, Long accountId) {
        LocalDateTime now = now();
        return jdbc.query(
                "SELECT id, role, account_id, function_code, operation, kind, metric_group, channel"
                        + " FROM permission_restriction WHERE " + target(role)
                        + " AND effective_from <= ? AND (effective_to IS NULL OR effective_to > ?) ORDER BY id",
                (rs, i) -> new RestrictionRow(
                        rs.getLong(1),
                        rs.getString(2),
                        (Long) rs.getObject(3, Long.class),
                        new PermissionKey(Function.valueOf(rs.getString(4)), Operation.valueOf(rs.getString(5))),
                        RestrictionKind.valueOf(rs.getString(6)),
                        rs.getString(7),
                        rs.getString(8)),
                role != null ? role : accountId,
                now,
                now);
    }

    public long insertRestriction(
            String role,
            Long accountId,
            PermissionKey key,
            RestrictionKind kind,
            String metricGroup,
            String channel,
            long actorId,
            String reason) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                con -> {
                    var ps = con.prepareStatement("""
                            INSERT INTO permission_restriction (applies_to, role, account_id, function_code, operation,
                                kind, metric_group, channel, effective_from, actor_id, reason)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """, new String[] {"id"});
                    ps.setString(1, role != null ? "ROLE" : "ACCOUNT");
                    ps.setString(2, role);
                    ps.setObject(3, accountId);
                    ps.setString(4, key.function().name());
                    ps.setString(5, key.operation().name());
                    ps.setString(6, kind.name());
                    ps.setString(7, metricGroup);
                    ps.setString(8, channel);
                    ps.setObject(9, now());
                    ps.setLong(10, actorId);
                    ps.setString(11, reason);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Đóng một dòng thu hẹp đang hiệu lực (khôi phục về mức ma trận). */
    public void closeRestriction(long id) {
        jdbc.update(
                "UPDATE permission_restriction SET effective_to = ? WHERE id = ? AND effective_to IS NULL", now(), id);
    }

    /** Quyền Duyệt kế hoạch chăm sóc gán thêm đang hiệu lực của tài khoản (FR-024). */
    public boolean hasActiveGrant(long accountId) {
        LocalDateTime now = now();
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM additional_grant WHERE account_id = ? AND effective_from <= ?"
                        + " AND (effective_to IS NULL OR effective_to > ?)",
                Integer.class,
                accountId,
                now,
                now);
        return rows != null && rows > 0;
    }

    public void insertCarePlanApprovalGrant(long accountId, long actorId, String reason) {
        jdbc.update(
                "INSERT INTO additional_grant (account_id, function_code, operation, effective_from, actor_id, reason)"
                        + " VALUES (?, 'CARE_PLAN', 'APPROVE', ?, ?, ?)",
                accountId,
                now(),
                actorId,
                reason);
    }

    public void closeGrant(long accountId) {
        jdbc.update(
                "UPDATE additional_grant SET effective_to = ? WHERE account_id = ? AND effective_to IS NULL",
                now(),
                accountId);
    }

    /**
     * Tài khoản Hoạt động đang có vai trò Quản lý viện; khóa các dòng gán vai trò để hai lệnh đồng thời không cùng thấy
     * "còn người khác" (FR-012; research A-05).
     */
    public List<Long> activeManagers() {
        return jdbc.queryForList(
                "SELECT ra.account_id FROM role_assignment ra JOIN account a ON a.id = ra.account_id"
                        + " WHERE ra.role = 'AC-01' AND ra.effective_to IS NULL AND a.status = 'ACTIVE' FOR UPDATE",
                Long.class);
    }

    private static String target(String role) {
        return role != null ? "applies_to = 'ROLE' AND role = ?" : "applies_to = 'ACCOUNT' AND account_id = ?";
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC);
    }
}
