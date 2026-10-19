package com.cambae.backend.m15.auditlog.api;

import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.PermissionKey;

/**
 * Khóa quyền xem nhật ký theo Phụ lục 27, dòng "Nhật ký thay đổi" (FR-049): Quản lý viện X (toàn viện), vai trò khác
 * P (trong phạm vi); người thân, người cao tuổi không có. Tra cứu toàn viện là kiểm {@link #VIEW} với
 * {@code Target.UNSPECIFIED}: chỉ phạm vi toàn viện đạt (002 research A-05).
 */
public final class AuditLogPermissions {

    public static final PermissionKey VIEW = new PermissionKey(Function.AUDIT_LOG, Operation.VIEW);

    private AuditLogPermissions() {}
}
