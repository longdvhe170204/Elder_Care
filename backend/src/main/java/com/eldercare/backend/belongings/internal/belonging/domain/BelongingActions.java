package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Hành động nhật ký của đồ gửi, bàn giao, đồng ý (FR-031a, DBR-23). */
public final class BelongingActions {

    public static final AuditAction BELONGING_INTAKE = new AuditAction("M12_BELONGING_INTAKE", false);
    public static final AuditAction BELONGING_HANDOVER = new AuditAction("M12_BELONGING_HANDOVER", false);
    public static final AuditAction SELF_CUSTODY_CONSENT_EXPIRED =
            new AuditAction("M12_SELF_CUSTODY_CONSENT_EXPIRED", false);
    public static final AuditAction SELF_CUSTODY_CONSENT_RECORDED =
            new AuditAction("M12_SELF_CUSTODY_CONSENT_RECORDED", false);
    public static final AuditAction SELF_CUSTODY_CONSENT_WITHDRAWN =
            new AuditAction("M12_SELF_CUSTODY_CONSENT_WITHDRAWN", true);

    private BelongingActions() {}
}
