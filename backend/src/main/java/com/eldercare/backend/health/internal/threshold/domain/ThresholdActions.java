package com.eldercare.backend.health.internal.threshold.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Mã nhật ký của lệnh ngưỡng (FR-022). */
public final class ThresholdActions {
    public static final AuditAction SET = new AuditAction("THRESHOLD_SET", false);
    public static final AuditAction CANCELLED = new AuditAction("THRESHOLD_CANCELLED", true);
    public static final AuditAction STOPPED = new AuditAction("THRESHOLD_STOPPED", true);

    private ThresholdActions() {}
}
