package com.cambae.backend.m15.account.internal.permission;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.spi.FootnoteCondition;
import com.cambae.backend.m15.account.internal.permission.PermissionViews.AccountPermissions;
import com.cambae.backend.m15.account.internal.permission.PermissionViews.EffectivePermission;
import com.cambae.backend.m15.account.internal.permission.PermissionViews.Restriction;
import com.cambae.backend.m15.account.internal.permission.PermissionViews.RestrictionKind;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        Set<String> registered =
                conditions.orderedStream().map(FootnoteCondition::footnoteCode).collect(Collectors.toSet());
        List<EffectivePermission> result = new ArrayList<>();
        roles.current(accountId)
                .forEach((key, grants) -> grants.stream()
                        .filter(g -> !FootnoteRules.blockedAtRoleLayer(g.footnotes(), registered))
                        .forEach(g -> result.add(new EffectivePermission(
                                key.function().name(),
                                key.operation(),
                                g.scope(),
                                g.footnotes().stream().sorted().toList(),
                                g.source()))));
        result.sort(Comparator.comparing(EffectivePermission::function).thenComparing(EffectivePermission::operation));
        return result;
    }

    public AccountPermissions of(long accountId, String status, List<String> roleCodes) {
        Instant now = time.now();
        LocalDateTime t = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        String active = " effective_from <= ? AND (effective_to IS NULL OR effective_to > ?)";
        List<Restriction> byRole = restrictions(
                "SELECT pr.id, pr.function_code, pr.operation, pr.kind, pr.effective_from, pr.reason"
                        + " FROM permission_restriction pr JOIN role_assignment ra ON ra.role = pr.role"
                        + " AND ra.account_id = ? AND ra.effective_to IS NULL WHERE pr.applies_to = 'ROLE'"
                        + " AND pr.effective_from <= ? AND (pr.effective_to IS NULL OR pr.effective_to > ?)",
                accountId,
                t,
                t);
        List<Restriction> byAccount = restrictions(
                "SELECT id, function_code, operation, kind, effective_from, reason FROM permission_restriction"
                        + " WHERE applies_to = 'ACCOUNT' AND account_id = ? AND" + active,
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
        return new AccountPermissions(accountId, now, status, roleCodes, byRole, byAccount, grants, current(accountId));
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
