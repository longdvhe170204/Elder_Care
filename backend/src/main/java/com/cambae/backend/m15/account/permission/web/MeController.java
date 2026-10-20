package com.cambae.backend.m15.account.permission.web;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.permission.internal.AccountPermissionsQuery;
import com.cambae.backend.m15.account.permission.internal.PermissionViews.EffectivePermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
    private final AccountApi accounts;
    private final AccountPermissionsQuery permissions;
    private final JdbcTemplate jdbc;

    MeController(
            ObjectProvider<CurrentUser> currentUser,
            AccountApi accounts,
            AccountPermissionsQuery permissions,
            JdbcTemplate jdbc) {
        this.currentUser = currentUser;
        this.accounts = accounts;
        this.permissions = permissions;
        this.jdbc = jdbc;
    }

    @GetMapping
    UserInfo me() {
        CurrentUser user = currentUser.getObject();
        Boolean mustChange = jdbc.queryForObject(
                "SELECT must_change_password FROM account WHERE id = ?", Boolean.class, user.accountId());
        return new UserInfo(
                user.accountId(),
                user.type(),
                accounts.fullName(user.accountId()).orElse(null),
                user.roles().stream().map(Role::code).sorted().toList(),
                Boolean.TRUE.equals(mustChange));
    }

    @GetMapping("/permissions")
    List<EffectivePermission> permissions() {
        return permissions.current(currentUser.getObject().accountId());
    }
}
