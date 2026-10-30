package com.eldercare.backend.administration.account.internal.permission.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Thuộc tính của vai trò ở {@code catalog_item} loại {@code M15.ROLE} (002 T098; mã mục là mã actor): loại tài khoản,
 * phạm vi của T, D (FR-034) và số phiên bản quyền, tăng khi thu hẹp theo vai trò (A-05). Nạp bằng migration.
 */
public record RoleAttributes(String accountType, PermissionScope executionScope, int permissionVersion) {

    public static final SharedKind<RoleAttributes> KIND =
            SharedKind.of("M15.ROLE", RoleAttributes.class).notDeletable();

    public RoleAttributes withNextVersion() {
        return new RoleAttributes(accountType, executionScope, permissionVersion + 1);
    }
}
