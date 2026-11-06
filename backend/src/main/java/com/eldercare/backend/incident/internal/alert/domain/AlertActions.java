package com.eldercare.backend.incident.internal.alert.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Mã nhật ký của lệnh xử lý cảnh báo. */
public final class AlertActions {
    public static final AuditAction RECEIVED = new AuditAction("ALERT_RECEIVED", false);
    public static final AuditAction HANDLING_STARTED = new AuditAction("ALERT_HANDLING_STARTED", false);
    public static final AuditAction LEVEL_CHANGED = new AuditAction("ALERT_LEVEL_CHANGED", true);
    public static final AuditAction CLOSED = new AuditAction("ALERT_CLOSED", false);
    public static final AuditAction ARRIVED = new AuditAction("ALERT_ARRIVED", false);
    public static final AuditAction CONVERTED = new AuditAction("ALERT_CONVERTED_TO_INCIDENT", false);
    public static final AuditAction ASSIGNED = new AuditAction("ALERT_ASSIGNED", false);
    public static final AuditAction CREATED_MANUALLY = new AuditAction("ALERT_CREATED_MANUALLY", false);

    private AlertActions() {}
}
