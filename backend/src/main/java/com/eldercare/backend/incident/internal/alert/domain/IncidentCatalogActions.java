package com.eldercare.backend.incident.internal.alert.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Hành động nhật ký của T006. */
public final class IncidentCatalogActions {
    public static final AuditAction TREND_RULE_UPDATED = new AuditAction("TREND_RULE_UPDATED", true);
    public static final AuditAction INCIDENT_TYPE_CREATED = new AuditAction("INCIDENT_TYPE_CREATED", false);
    public static final AuditAction INCIDENT_TYPE_UPDATED = new AuditAction("INCIDENT_TYPE_UPDATED", false);
    public static final AuditAction PROGRESSION_TYPE_CREATED = new AuditAction("PROGRESSION_TYPE_CREATED", false);

    private IncidentCatalogActions() {}
}
