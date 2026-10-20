package com.cambae.backend.m15.approval.internal.service;

import com.cambae.backend.m15.approval.internal.domain.ApprovalActions;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Áp dụng một yêu cầu; lỗi khi áp dụng thì mọi tác động đã bỏ (transaction riêng rollback), yêu cầu giữ Đã duyệt, nhật
 * ký lỗi kèm nguyên nhân ghi ở transaction riêng để lần sau áp dụng lại (FR-037b).
 */
@Component
public class RequestApplier {

    private static final Logger LOG = LoggerFactory.getLogger(RequestApplier.class);

    private final ApplyTransaction transaction;
    private final AuditLogApi auditLog;

    RequestApplier(ApplyTransaction transaction, AuditLogApi auditLog) {
        this.transaction = transaction;
        this.auditLog = auditLog;
    }

    /** Trả {@code true} khi lần này đã xử lý xong (áp dụng hoặc áp dụng không thành). */
    public boolean applySafely(long id, Long schedulerRunId) {
        try {
            return transaction.apply(id, schedulerRunId) != ApplyTransaction.Outcome.NOT_APPLICABLE;
        } catch (RuntimeException e) {
            LOG.warn("Áp dụng yêu cầu {} lỗi, giữ Đã duyệt", id, e);
            auditLog.writeOutsideTransaction(AuditLogEntry.builder()
                    .action(ApprovalActions.APPLY_ERROR)
                    .entity(ApprovalActions.ENTITY, id)
                    .basis(new Basis(null, id, null, schedulerRunId))
                    .details(Map.of("cause", String.valueOf(e.getMessage())))
                    .build());
            return false;
        }
    }
}
