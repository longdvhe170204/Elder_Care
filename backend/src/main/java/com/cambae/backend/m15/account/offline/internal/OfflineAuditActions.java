package com.cambae.backend.m15.account.offline.internal;

import com.cambae.backend.m15.auditlog.api.AuditAction;

/** Hành động nhật ký của bản ghi chờ xem lại (data-model mục 6: {@code NT_CHO_XEM_LAI}, {@code NT_CHAP_NHAN}, {@code NT_TU_CHOI}). */
final class OfflineAuditActions {

    static final AuditAction OFFLINE_PENDING_REVIEW = new AuditAction("OFFLINE_PENDING_REVIEW", false);
    static final AuditAction OFFLINE_RECORD_ACCEPTED = new AuditAction("OFFLINE_RECORD_ACCEPTED", false);
    static final AuditAction OFFLINE_RECORD_REJECTED = new AuditAction("OFFLINE_RECORD_REJECTED", true);

    /** Đối tượng nhật ký của bản ghi chờ xem lại. */
    static final String ENTITY = "M15.PENDING_REVIEW_RECORD";

    private OfflineAuditActions() {}
}
