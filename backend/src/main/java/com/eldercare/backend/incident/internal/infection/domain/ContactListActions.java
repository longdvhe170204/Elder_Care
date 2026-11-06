package com.eldercare.backend.incident.internal.infection.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Mã nhật ký của các lệnh danh sách tiếp xúc (bảng FR-061). */
public final class ContactListActions {
    public static final AuditAction CREATED = new AuditAction("CONTACT_LIST_CREATED", false);
    public static final AuditAction CONTACT_ADDED = new AuditAction("CONTACT_ADDED", false);
    public static final AuditAction CONTACT_REMOVED = new AuditAction("CONTACT_REMOVED", false);
    public static final AuditAction CONTACT_EXCLUDED = new AuditAction("CONTACT_EXCLUDED", true);
    public static final AuditAction CONFIRMED = new AuditAction("CONTACT_LIST_CONFIRMED", false);
    public static final AuditAction ENDED = new AuditAction("CONTACT_LIST_ENDED", false);

    private ContactListActions() {}
}
