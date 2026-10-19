package com.cambae.backend.m15.auditlog.api;

import com.cambae.backend.m15.account.api.PermissionKey;

/**
 * Khóa quyền xem nhật ký theo Phụ lục 27, dòng "Nhật ký thay đổi" (FR-049). Tạm dùng chuỗi như mọi khóa của 000; 002
 * đổi sang enum và cấp theo ma trận quyền.
 */
public final class AuditLogPermissions {

    /** Tra cứu toàn viện: cột X, chỉ Quản lý viện. */
    public static final PermissionKey VIEW_FACILITY = new PermissionKey("AUDIT_LOG", "VIEW_FACILITY");

    /** Xem lịch sử một đối tượng trong phạm vi: cột P; người thân, người cao tuổi không có. */
    public static final PermissionKey VIEW = new PermissionKey("AUDIT_LOG", "VIEW");

    private AuditLogPermissions() {}
}
