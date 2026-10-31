package com.eldercare.backend.administration.account.internal.offline.domain;

import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;

/** Hành động nhật ký của bản ghi chờ xem lại (data-model mục 6: {@code NT_CHO_XEM_LAI}, {@code NT_CHAP_NHAN}, {@code NT_TU_CHOI}). */
public final class OfflineAuditActions {

    public static final AuditAction OFFLINE_PENDING_REVIEW = new AuditAction("OFFLINE_PENDING_REVIEW", false);
    public static final AuditAction OFFLINE_RECORD_ACCEPTED = new AuditAction("OFFLINE_RECORD_ACCEPTED", false);
    public static final AuditAction OFFLINE_RECORD_REJECTED = new AuditAction("OFFLINE_RECORD_REJECTED", true);

    /** Đối tượng nhật ký của bản ghi chờ xem lại. */
    public static final String ENTITY = "M15.PENDING_REVIEW_RECORD";

    private OfflineAuditActions() {}
}
