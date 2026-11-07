package com.eldercare.backend.incident.internal.emergency.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Mã nhật ký của quy trình khẩn cấp và chuyển viện (FR-048a, FR-049, FR-050h; 19.4). */
public final class EmergencyActions {
    public static final AuditAction WISH_CHECKED = new AuditAction("WISH_CHECK_CONFIRMED", false);
    public static final AuditAction TRANSFERRED_OUT = new AuditAction("INCIDENT_TRANSFERRED_OUT", false);
    public static final AuditAction SUMMARY_PRINTED = new AuditAction("TRANSFER_SUMMARY_PRINTED", false);

    private EmergencyActions() {}
}
