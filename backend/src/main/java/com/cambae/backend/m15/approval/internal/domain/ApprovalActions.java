package com.cambae.backend.m15.approval.internal.domain;

import com.cambae.backend.m15.auditlog.api.AuditAction;

/** Hành động nhật ký của yêu cầu phê duyệt (data-model mục 3, cột "Nhật ký"; FR-041, FR-044). */
public final class ApprovalActions {

    public static final String ENTITY = "M15.APPROVAL_REQUEST";

    public static final AuditAction CREATED = new AuditAction("REQUEST_CREATED", true);
    public static final AuditAction DRAFT_EDITED = new AuditAction("REQUEST_DRAFT_EDITED", true);
    public static final AuditAction SUBMITTED = new AuditAction("REQUEST_SUBMITTED", false);
    /** Lý do bắt buộc khi tự duyệt, kiểm ở lệnh (FR-035). */
    public static final AuditAction APPROVED = new AuditAction("REQUEST_APPROVED", false);

    public static final AuditAction REJECTED = new AuditAction("REQUEST_REJECTED", true);
    public static final AuditAction CANCELLED = new AuditAction("REQUEST_CANCELLED", true);
    public static final AuditAction APPLIED = new AuditAction("REQUEST_APPLIED", false);
    public static final AuditAction APPLY_FAILED = new AuditAction("REQUEST_APPLY_FAILED", false);
    public static final AuditAction APPLY_ERROR = new AuditAction("REQUEST_APPLY_ERROR", false);
    public static final AuditAction REMINDED = new AuditAction("REQUEST_REMINDED", false);
    public static final AuditAction ESCALATED = new AuditAction("REQUEST_ESCALATED", false);
    public static final AuditAction EXTENDED_TRANSITION = new AuditAction("REQUEST_EXTENDED_TRANSITION", false);
    public static final AuditAction CLOSED_FOR_FINAL_STATE = new AuditAction("REQUEST_CLOSED_FOR_FINAL_STATE", false);

    private ApprovalActions() {}
}
