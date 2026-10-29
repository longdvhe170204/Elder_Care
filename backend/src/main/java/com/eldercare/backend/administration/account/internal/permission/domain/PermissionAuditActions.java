package com.eldercare.backend.administration.account.internal.permission.domain;

import com.eldercare.backend.administration.auditlog.api.AuditAction;

/**
 * Hành động nhật ký đổi quyền (002 US7; data-model mục 6, glossary mục 7): {@code QUYEN_THU_HEP},
 * {@code QUYEN_KHOI_PHUC}, {@code QUYEN_GAN_THEM}, {@code QUYEN_GO_THEM}. Thu hẹp theo vai trò ghi trên đối tượng
 * {@link #ROLE_ENTITY}, theo tài khoản và quyền gán thêm trên {@code M15.ACCOUNT}.
 */
public final class PermissionAuditActions {

    public static final AuditAction PERMISSION_RESTRICTED = new AuditAction("PERMISSION_RESTRICTED", true);
    public static final AuditAction PERMISSION_RESTORED = new AuditAction("PERMISSION_RESTORED", true);
    public static final AuditAction ADDITIONAL_PERMISSION_GRANTED =
            new AuditAction("ADDITIONAL_PERMISSION_GRANTED", true);
    public static final AuditAction ADDITIONAL_PERMISSION_REVOKED =
            new AuditAction("ADDITIONAL_PERMISSION_REVOKED", true);

    /** Đối tượng nhật ký của vai trò; mã đối tượng là mã vai trò (AC-xx). */
    public static final String ROLE_ENTITY = "M15.ROLE";

    private PermissionAuditActions() {}
}
