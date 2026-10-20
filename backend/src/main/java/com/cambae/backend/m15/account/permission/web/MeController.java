package com.cambae.backend.m15.account.permission.web;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.AccountType;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FootnoteCondition;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.permission.internal.FootnoteRules;
import com.cambae.backend.m15.account.permission.internal.GrantSource;
import com.cambae.backend.m15.account.permission.internal.PermissionScope;
import com.cambae.backend.m15.account.permission.internal.RolePermissionCalculator;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /me}, {@code GET /me/permissions} (002 T025; FR-042; contracts/api-002.yaml): người dùng của phiên và quyền
 * hiện hành để client ẩn hiện chức năng; backend luôn kiểm lại ở lệnh (constitution XI). Quyền liệt kê là phương án
 * vai trò không vướng chú thích nhiệm vụ hay chú thích điều kiện chưa có bản cài; quyền theo nhiệm vụ thêm ở T053.
 */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "auth")
class MeController {

    private final ObjectProvider<CurrentUser> currentUser;
    private final RolePermissionCalculator roles;
    private final ObjectProvider<FootnoteCondition> conditions;
    private final JdbcTemplate jdbc;

    MeController(
            ObjectProvider<CurrentUser> currentUser,
            RolePermissionCalculator roles,
            ObjectProvider<FootnoteCondition> conditions,
            JdbcTemplate jdbc) {
        this.currentUser = currentUser;
        this.roles = roles;
        this.conditions = conditions;
        this.jdbc = jdbc;
    }

    /** Schema {@code UserInfo}; {@code fullName} có từ T032a (họ tên theo hồ sơ liên kết). */
    @Schema(name = "UserInfo")
    record UserInfo(
            long accountId, AccountType type, String fullName, List<String> roles, boolean mustChangePassword) {}

    /** Schema {@code EffectivePermission}. */
    @Schema(name = "EffectivePermission")
    record EffectivePermission(
            String function,
            Operation operation,
            PermissionScope scope,
            List<String> footnoteCodes,
            GrantSource source) {}

    @GetMapping
    UserInfo me() {
        CurrentUser user = currentUser.getObject();
        Boolean mustChange = jdbc.queryForObject(
                "SELECT must_change_password FROM account WHERE id = ?", Boolean.class, user.accountId());
        return new UserInfo(
                user.accountId(),
                user.type(),
                null,
                user.roles().stream().map(Role::code).sorted().toList(),
                Boolean.TRUE.equals(mustChange));
    }

    @GetMapping("/permissions")
    List<EffectivePermission> permissions() {
        CurrentUser user = currentUser.getObject();
        Set<String> registered =
                conditions.orderedStream().map(FootnoteCondition::footnoteCode).collect(Collectors.toSet());
        List<EffectivePermission> result = new ArrayList<>();
        roles.current(user.accountId())
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
}
