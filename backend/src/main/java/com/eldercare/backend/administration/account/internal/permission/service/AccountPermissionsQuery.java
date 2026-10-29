package com.eldercare.backend.administration.account.internal.permission.service;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import com.eldercare.backend.administration.account.internal.permission.domain.FootnoteRules;
import com.eldercare.backend.administration.account.internal.permission.domain.GrantSource;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionScope;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.AccountPermissions;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.EffectivePermission;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.Restriction;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.RestrictionKind;
import com.eldercare.backend.administration.account.internal.permission.domain.RoleGrant;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Dựng tập quyền hiện hành của một tài khoản cho {@code /me/permissions} và phản hồi {@code AccountPermissions} (FR-026,
 * FR-042): phương án vai trò không vướng chú thích nhiệm vụ hay chú thích điều kiện chưa có bản cài
 * ({@link FootnoteRules}), thu hẹp theo vai trò, theo tài khoản và quyền gán thêm đang hiệu lực.
 */
@Component
public class AccountPermissionsQuery {

    private static final Comparator<EffectivePermission> ORDER = Comparator.comparing(EffectivePermission::function)
            .thenComparing(EffectivePermission::operation)
            .thenComparing(EffectivePermission::scope);

    private final RolePermissionCalculator roles;
    private final ObjectProvider<FootnoteCondition> conditions;
    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    AccountPermissionsQuery(
            RolePermissionCalculator roles,
            ObjectProvider<FootnoteCondition> conditions,
            JdbcTemplate jdbc,
            BusinessTime time) {
        this.roles = roles;
        this.conditions = conditions;
        this.jdbc = jdbc;
        this.time = time;
    }

    /** Quyền hiện hành, sắp theo chức năng rồi thao tác. */
    public List<EffectivePermission> current(long accountId) {
        return effective(roles.current(accountId));
    }

    /** Phương án không vướng chú thích chặn ở lớp vai trò ({@link FootnoteRules}), sắp như {@link #current}. */
    public List<EffectivePermission> effective(Map<PermissionKey, Set<RoleGrant>> permissions) {
        Set<String> registered =
                conditions.orderedStream().map(FootnoteCondition::footnoteCode).collect(Collectors.toSet());
        List<EffectivePermission> result = new ArrayList<>();
        permissions.forEach((key, grants) -> grants.stream()
                .filter(g -> !FootnoteRules.blockedAtRoleLayer(g.footnotes(), registered))
                .forEach(g -> result.add(view(key, g))));
        result.sort(ORDER);
        return result;
    }

    /** Mọi phương án, kể cả vướng chú thích chặn: quyền tối đa, quyền vai trò sau thu hẹp (FR-018, FR-020). */
    public static List<EffectivePermission> all(Map<PermissionKey, Set<RoleGrant>> permissions) {
        List<EffectivePermission> result = new ArrayList<>();
        permissions.forEach((key, grants) -> grants.forEach(g -> result.add(view(key, g))));
        result.sort(ORDER);
        return result;
    }

    public AccountPermissions of(long accountId, String status, List<String> roleCodes) {
        return at(accountId, time.now(), status, roleCodes, current(accountId));
    }

    /**
     * Tập quyền tại {@code at} (FR-026a, BR-M15-16): thu hẹp, quyền gán thêm đang hiệu lực tại {@code at}; thu hẹp theo
     * vai trò chỉ của vai trò đang gán tại {@code at}.
     */
    public AccountPermissions at(long accountId, Instant at, String status, List<String> roleCodes) {
        return at(accountId, at, status, roleCodes, effective(roles.at(accountId, at)));
    }

    private AccountPermissions at(
            long accountId, Instant at, String status, List<String> roleCodes, List<EffectivePermission> current) {
        LocalDateTime t = LocalDateTime.ofInstant(at, ZoneOffset.UTC);
        String active = " effective_from <= ? AND (effective_to IS NULL OR effective_to > ?)";
        List<Restriction> byRole = restrictions(
                "SELECT pr.id, pr.function_code, pr.operation, pr.kind, pr.effective_from, pr.reason"
                        + " FROM permission_restriction pr JOIN role_assignment ra ON ra.role = pr.role"
                        + " AND ra.account_id = ? AND ra.effective_from <= ?"
                        + " AND (ra.effective_to IS NULL OR ra.effective_to > ?) WHERE pr.applies_to = 'ROLE'"
                        + " AND pr.effective_from <= ? AND (pr.effective_to IS NULL OR pr.effective_to > ?)"
                        + " ORDER BY pr.id",
                accountId,
                t,
                t,
                t,
                t);
        List<Restriction> byAccount = restrictions(
                "SELECT id, function_code, operation, kind, effective_from, reason FROM permission_restriction"
                        + " WHERE applies_to = 'ACCOUNT' AND account_id = ? AND" + active + " ORDER BY id",
                accountId,
                t,
                t);
        List<EffectivePermission> grants = jdbc.query(
                "SELECT function_code, operation FROM additional_grant WHERE account_id = ? AND" + active,
                (rs, i) -> new EffectivePermission(
                        Function.valueOf(rs.getString(1)).name(),
                        Operation.valueOf(rs.getString(2)),
                        PermissionScope.ASSIGNED,
                        List.of(),
                        GrantSource.ADDITIONAL_GRANT),
                accountId,
                t,
                t);
        return new AccountPermissions(accountId, at, status, roleCodes, byRole, byAccount, grants, current);
    }

    /** Thu hẹp theo vai trò đang hiệu lực của một vai trò, theo thứ tự lập. */
    public List<Restriction> roleRestrictions(String role) {
        LocalDateTime t = LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC);
        return restrictions(
                "SELECT id, function_code, operation, kind, effective_from, reason FROM permission_restriction"
                        + " WHERE applies_to = 'ROLE' AND role = ? AND effective_from <= ?"
                        + " AND (effective_to IS NULL OR effective_to > ?) ORDER BY id",
                role,
                t,
                t);
    }

    private static EffectivePermission view(PermissionKey key, RoleGrant g) {
        return new EffectivePermission(
                key.function().name(),
                key.operation(),
                g.scope(),
                g.footnotes().stream().sorted().toList(),
                g.source());
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
}
