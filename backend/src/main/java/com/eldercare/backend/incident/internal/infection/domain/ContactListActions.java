package com.eldercare.backend.incident.internal.infection.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Mã nhật ký của lệnh lập danh sách tiếp xúc. */
public final class ContactListActions {
    public static final AuditAction CREATED = new AuditAction("CONTACT_LIST_CREATED", false);

    private ContactListActions() {}
}
