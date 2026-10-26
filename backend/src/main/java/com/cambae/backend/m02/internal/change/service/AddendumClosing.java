package com.cambae.backend.m02.internal.change.service;

import com.cambae.backend.m02.internal.change.domain.Addendum;
import com.cambae.backend.m02.internal.change.persistence.AddendumStore;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Đóng phụ lục khi yêu cầu của nó không còn được áp dụng (FR-042, FR-045; data-model mục 7): phụ lục Chờ hiệu lực chuyển
 * Không áp dụng và thuộc tính được nhả để yêu cầu khác dùng (FR-046). Dùng ở ba chỗ: yêu cầu Áp dụng không thành, yêu cầu Đã
 * duyệt bị hủy, yêu cầu bị thay thế bởi đánh giá. Chạy trong transaction của chỗ gọi.
 */
@Component
class AddendumClosing {

    private final AddendumStore addenda;
    private final AuditLogApi auditLog;

    AddendumClosing(AddendumStore addenda, AuditLogApi auditLog) {
        this.addenda = addenda;
        this.auditLog = auditLog;
    }

    void close(long approvalRequestId, long residentId, Basis basis) {
        addenda.setPending(approvalRequestId, false);
        addenda.lockByRequest(approvalRequestId)
                .filter(addendum -> addendum.status() == Addendum.Status.PENDING_EFFECTIVE)
                .ifPresent(addendum -> {
                    addenda.markNotApplied(addendum.id());
                    auditLog.write(AuditLogEntry.builder()
                            .action(ResidencyChangeHandler.ADDENDUM_NOT_APPLIED)
                            .entity(ResidencyChangeHandler.ADDENDUM_ENTITY, addendum.id())
                            .resident(residentId)
                            .before(Map.of("status", Addendum.Status.PENDING_EFFECTIVE.name()))
                            .after(Map.of("status", Addendum.Status.NOT_APPLIED.name()))
                            .basis(basis)
                            .build());
                });
    }
}
