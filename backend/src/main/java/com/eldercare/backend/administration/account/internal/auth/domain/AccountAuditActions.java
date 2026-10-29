package com.eldercare.backend.administration.account.internal.auth.domain;

import com.eldercare.backend.administration.auditlog.api.AuditAction;

/** Hành động nhật ký của tài khoản (data-model mục 6; glossary mục 7): {@code TK_*}, {@code VT_*}. */
public final class AccountAuditActions {

    public static final AuditAction ACCOUNT_CREATED = new AuditAction("ACCOUNT_CREATED", false);
    public static final AuditAction ACCOUNT_LOCKED = new AuditAction("ACCOUNT_LOCKED", false);
    public static final AuditAction ACCOUNT_UNLOCKED = new AuditAction("ACCOUNT_UNLOCKED", true);
    public static final AuditAction PASSWORD_RESET = new AuditAction("PASSWORD_RESET", true);
    public static final AuditAction ROLE_ASSIGNED = new AuditAction("ROLE_ASSIGNED", true);
    public static final AuditAction ROLE_REMOVED = new AuditAction("ROLE_REMOVED", true);

    // Đăng nhập (FR-015, research A-03): không đổi dữ liệu nghiệp vụ, trước/sau để trống (000 FR-043a)
    public static final AuditAction LOGIN = new AuditAction("LOGIN", false);
    public static final AuditAction LOGIN_FAILED = new AuditAction("LOGIN_FAILED", false);
    public static final AuditAction ACCOUNT_TEMPORARILY_LOCKED = new AuditAction("ACCOUNT_TEMPORARILY_LOCKED", false);
    public static final AuditAction TEMPORARY_LOCK_EXPIRED = new AuditAction("TEMPORARY_LOCK_EXPIRED", false);
    public static final AuditAction ACCOUNT_EARLY_UNLOCKED = new AuditAction("ACCOUNT_EARLY_UNLOCKED", true);
    public static final AuditAction ACCOUNT_INACTIVE = new AuditAction("ACCOUNT_INACTIVE", false);
    public static final AuditAction ACCOUNT_REACTIVATED = new AuditAction("ACCOUNT_REACTIVATED", true);
    public static final AuditAction PASSWORD_CHANGED = new AuditAction("PASSWORD_CHANGED", false);

    // Tài khoản người cao tuổi (US8; data-model mục 2): QR_TAO, QR_DUNG, TB_GAN, TB_GO
    public static final AuditAction LOGIN_QR_CREATED = new AuditAction("LOGIN_QR_CREATED", false);
    public static final AuditAction LOGIN_QR_USED = new AuditAction("LOGIN_QR_USED", false);
    public static final AuditAction DEVICE_BOUND = new AuditAction("DEVICE_BOUND", false);
    public static final AuditAction DEVICE_UNBOUND = new AuditAction("DEVICE_UNBOUND", false);

    /** Đối tượng nhật ký của tài khoản. */
    public static final String ENTITY = "M15.ACCOUNT";

    private AccountAuditActions() {}
}
