package com.eldercare.backend.incident.internal.incident.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Mã nhật ký của lệnh xử lý sự cố. */
public final class IncidentActions {
    public static final AuditAction LEVEL_CHANGED = new AuditAction("INCIDENT_LEVEL_CHANGED", true);

    private IncidentActions() {}
}
