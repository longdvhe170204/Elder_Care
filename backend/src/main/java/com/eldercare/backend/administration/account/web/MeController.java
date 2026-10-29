package com.eldercare.backend.administration.account.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.internal.auth.service.AccountService;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.EffectivePermission;
import com.eldercare.backend.administration.account.internal.permission.service.AccountPermissionsQuery;
import com.eldercare.backend.administration.account.web.response.UserInfo;
import com.eldercare.backend.common.Role;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
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
    private final AccountService accountService;

    MeController(
            ObjectProvider<CurrentUser> currentUser,
            AccountApi accounts,
            AccountPermissionsQuery permissions,
            AccountService accountService) {
        this.currentUser = currentUser;
        this.accounts = accounts;
        this.permissions = permissions;
        this.accountService = accountService;
    }

    @GetMapping
    UserInfo me() {
        CurrentUser user = currentUser.getObject();
        boolean mustChange = accountService.mustChangePassword(user.accountId());
        return new UserInfo(
                user.accountId(),
                user.type(),
                accounts.fullName(user.accountId()).orElse(null),
                user.roles().stream().map(Role::code).sorted().toList(),
                mustChange);
    }

    @GetMapping("/permissions")
    List<EffectivePermission> permissions() {
        return permissions.current(currentUser.getObject().accountId());
    }
}
