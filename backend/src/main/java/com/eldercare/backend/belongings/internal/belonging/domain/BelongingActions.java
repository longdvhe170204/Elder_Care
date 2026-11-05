package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Hành động nhật ký của đồ gửi, bàn giao, đồng ý (FR-031a, DBR-23). */
public final class BelongingActions {

    public static final AuditAction BELONGING_INTAKE = new AuditAction("M12_BELONGING_INTAKE", false);
    public static final AuditAction BELONGING_HANDOVER = new AuditAction("M12_BELONGING_HANDOVER", false);
    public static final AuditAction BELONGING_INCIDENT_LINKED = new AuditAction("M12_BELONGING_INCIDENT_LINKED", false);
    public static final AuditAction SELF_CUSTODY_CONSENT_EXPIRED =
            new AuditAction("M12_SELF_CUSTODY_CONSENT_EXPIRED", false);
    public static final AuditAction SELF_CUSTODY_CONSENT_RECORDED =
            new AuditAction("M12_SELF_CUSTODY_CONSENT_RECORDED", false);
    public static final AuditAction SELF_CUSTODY_CONSENT_WITHDRAWN =
            new AuditAction("M12_SELF_CUSTODY_CONSENT_WITHDRAWN", true);

    public static final AuditAction RETURN_BLOCKED = new AuditAction("M12_RETURN_BLOCKED", false);
    public static final AuditAction RECIPIENT_CONFIRMATION_CREATED =
            new AuditAction("M12_RECIPIENT_CONF_CREATED", false);
    public static final AuditAction RECIPIENT_CONFIRMATION_CONFIRMED =
            new AuditAction("M12_RECIPIENT_CONF_CONFIRMED", false);
    public static final AuditAction RECIPIENT_CONFIRMATION_REJECTED =
            new AuditAction("M12_RECIPIENT_CONF_REJECTED", true);
    public static final AuditAction RECIPIENT_CONFIRMATION_ON_BEHALF =
            new AuditAction("M12_RECIPIENT_CONF_ON_BEHALF", true);
    public static final AuditAction RECIPIENT_CONFIRMATION_CANCELLED =
            new AuditAction("M12_RECIPIENT_CONF_CANCELLED", true);
    public static final AuditAction RECIPIENT_CONFIRMATION_USED = new AuditAction("M12_RECIPIENT_CONF_USED", false);
    public static final AuditAction CONTACT_ATTEMPT_RECORDED = new AuditAction("M12_CONTACT_ATTEMPT", false);

    private BelongingActions() {}
}
