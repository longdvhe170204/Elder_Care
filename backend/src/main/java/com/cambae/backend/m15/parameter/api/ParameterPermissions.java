package com.cambae.backend.m15.parameter.api;

import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;

/**
 * Khóa quyền tham số theo Phụ lục 27, dòng "Tài khoản, phân quyền, tham số": cột C chỉ Quản lý viện (chú thích ¹²:
 * tham số chỉ Quản lý viện; BR-M15-04, FR-013).
 */
public final class ParameterPermissions {

    public static final PermissionKey VIEW = new PermissionKey(Function.ACCOUNT_PERMISSION_PARAMETER, Operation.VIEW);
    public static final PermissionKey CHANGE =
            new PermissionKey(Function.ACCOUNT_PERMISSION_PARAMETER, Operation.UPDATE);

    private ParameterPermissions() {}
}
