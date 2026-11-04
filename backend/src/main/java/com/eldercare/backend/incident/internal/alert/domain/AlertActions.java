package com.eldercare.backend.incident.internal.alert.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Mã nhật ký của lệnh xử lý cảnh báo. */
public final class AlertActions {
    public static final AuditAction RECEIVED = new AuditAction("ALERT_RECEIVED", false);

    private AlertActions() {}
}
