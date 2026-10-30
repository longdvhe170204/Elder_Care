package com.eldercare.backend.administration.account.internal.permission.service;

import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import com.eldercare.backend.administration.account.internal.permission.domain.FootnoteRules;
import com.eldercare.backend.administration.account.internal.permission.domain.GrantSource;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionScope;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.AccountPermissions;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.EffectivePermission;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.Restriction;
import com.eldercare.backend.administration.account.internal.permission.domain.RoleGrant;
import com.eldercare.backend.administration.account.internal.permission.persistence.PermissionReads;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
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
    private final PermissionReads reads;
    private final BusinessTime time;

    AccountPermissionsQuery(
            RolePermissionCalculator roles,
            ObjectProvider<FootnoteCondition> conditions,
            PermissionReads reads,
            BusinessTime time) {
        this.roles = roles;
        this.conditions = conditions;
        this.reads = reads;
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
        List<EffectivePermission> grants = reads.grants(accountId, at).stream()
                .map(k -> new EffectivePermission(
                        k.function().name(),
                        k.operation(),
                        PermissionScope.ASSIGNED,
                        List.of(),
                        GrantSource.ADDITIONAL_GRANT))
                .toList();
        return new AccountPermissions(
                accountId,
                at,
                status,
                roleCodes,
                reads.roleRestrictions(accountId, at),
                reads.accountRestrictions(accountId, at),
                grants,
                current);
    }

    /** Thu hẹp theo vai trò đang hiệu lực của một vai trò, theo thứ tự lập. */
    public List<Restriction> roleRestrictions(String role) {
        return reads.restrictionsOfRole(role, time.now());
    }

    private static EffectivePermission view(PermissionKey key, RoleGrant g) {
        return new EffectivePermission(
                key.function().name(),
                key.operation(),
                g.scope(),
                g.footnotes().stream().sorted().toList(),
                g.source());
    }
}
