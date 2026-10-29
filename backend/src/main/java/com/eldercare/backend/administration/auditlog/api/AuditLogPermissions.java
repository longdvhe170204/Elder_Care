package com.eldercare.backend.administration.auditlog.api;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;

/**
 * Khóa quyền xem nhật ký theo Phụ lục 27, dòng "Nhật ký thay đổi" (FR-049): Quản lý viện X (toàn viện), vai trò khác
 * P (trong phạm vi); người thân, người cao tuổi không có. Tra cứu toàn viện là kiểm {@link #VIEW} với
 * {@code Target.UNSPECIFIED}: chỉ phạm vi toàn viện đạt (002 research A-05).
 */
public final class AuditLogPermissions {

    public static final PermissionKey VIEW = new PermissionKey(Function.AUDIT_LOG, Operation.VIEW);

    private AuditLogPermissions() {}
}
