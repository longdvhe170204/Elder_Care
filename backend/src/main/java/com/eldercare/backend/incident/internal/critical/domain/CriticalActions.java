package com.eldercare.backend.incident.internal.critical.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Mã nhật ký của lệnh nguy kịch (FR-050a → FR-050d). */
public final class CriticalActions {
    public static final AuditAction FLAG_RECORDED = new AuditAction("CRITICAL_FLAG_RECORDED", false);
    public static final AuditAction FLAG_CONFIRMED = new AuditAction("CRITICAL_FLAG_CONFIRMED", false);
    public static final AuditAction FLAG_REMOVED = new AuditAction("CRITICAL_FLAG_REMOVED", true);
    public static final AuditAction WISH_RECONFIRMED = new AuditAction("WISH_RECONFIRMATION_RECORDED", false);
    public static final AuditAction CHOICE_EXECUTED = new AuditAction("CHOICE_EXECUTION_RECORDED", false);

    private CriticalActions() {}
}
