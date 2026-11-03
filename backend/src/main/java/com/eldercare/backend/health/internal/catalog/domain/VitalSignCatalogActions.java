package com.eldercare.backend.health.internal.catalog.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Nhật ký lệnh danh mục M06, FR-002. */
public final class VitalSignCatalogActions {
    public static final AuditAction CREATED = new AuditAction("VITAL_SIGN_TYPE_CREATED", false);
    public static final AuditAction UPDATED = new AuditAction("VITAL_SIGN_TYPE_UPDATED", false);
    public static final AuditAction DEACTIVATED = new AuditAction("VITAL_SIGN_TYPE_DEACTIVATED", true);

    private VitalSignCatalogActions() {}
}
