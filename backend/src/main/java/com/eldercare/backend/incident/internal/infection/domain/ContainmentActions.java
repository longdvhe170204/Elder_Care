package com.eldercare.backend.incident.internal.infection.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Danh mục hành động khoanh vùng của M05. */
public final class ContainmentActions {
    public static final AuditAction CREATED = new AuditAction("CONTAINMENT_CREATED", true);
    public static final AuditAction LIFTED = new AuditAction("CONTAINMENT_LIFTED", true);
    public static final AuditAction ATTACHED = new AuditAction("CONTAINMENT_INCIDENT_ATTACHED", false);

    private ContainmentActions() {}
}
