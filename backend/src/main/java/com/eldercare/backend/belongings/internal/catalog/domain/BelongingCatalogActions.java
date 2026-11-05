package com.eldercare.backend.belongings.internal.catalog.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Hành động nhật ký của danh mục M12 (FR-031a). */
public final class BelongingCatalogActions {

    public static final AuditAction BELONGING_TYPE_CREATED = new AuditAction("M12_BELONGING_TYPE_CREATED", false);
    public static final AuditAction BELONGING_TYPE_UPDATED = new AuditAction("M12_BELONGING_TYPE_UPDATED", false);
    public static final AuditAction CONDITION_LEVEL_CREATED = new AuditAction("M12_CONDITION_LEVEL_CREATED", false);
    public static final AuditAction CONDITION_LEVEL_UPDATED = new AuditAction("M12_CONDITION_LEVEL_UPDATED", false);
    public static final AuditAction STORAGE_LOCATION_SAVED = new AuditAction("M12_STORAGE_LOCATION_SAVED", false);

    private BelongingCatalogActions() {}
}
