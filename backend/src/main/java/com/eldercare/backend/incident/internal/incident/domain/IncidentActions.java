package com.eldercare.backend.incident.internal.incident.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Mã nhật ký của lệnh xử lý sự cố. */
public final class IncidentActions {
    public static final AuditAction LEVEL_CHANGED = new AuditAction("INCIDENT_LEVEL_CHANGED", true);
    public static final AuditAction RECORDED = new AuditAction("INCIDENT_RECORDED", false);
    public static final AuditAction POST_FALL_MONITORING_ENDED = new AuditAction("POST_FALL_MONITORING_ENDED", true);
    public static final AuditAction PROGRESSION_ADDED = new AuditAction("INCIDENT_PROGRESSION_ADDED", false);
    public static final AuditAction MOVED_TO_MONITORING = new AuditAction("INCIDENT_MOVED_TO_MONITORING", false);
    public static final AuditAction REOPENED = new AuditAction("INCIDENT_REOPENED", true);
    public static final AuditAction CLOSED = new AuditAction("INCIDENT_CLOSED", false);
    public static final AuditAction JOINED = new AuditAction("INCIDENT_HANDLING_JOINED", false);
    public static final AuditAction ASSIGNED = new AuditAction("INCIDENT_HANDLER_ASSIGNED", false);
    public static final AuditAction RECEIVED = new AuditAction("INCIDENT_RECEIVED", false);

    private IncidentActions() {}
}
