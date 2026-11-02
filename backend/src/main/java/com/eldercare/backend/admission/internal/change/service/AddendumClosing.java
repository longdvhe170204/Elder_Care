package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.admission.internal.change.domain.Addendum;
import com.eldercare.backend.admission.internal.change.domain.ChangeType;
import com.eldercare.backend.admission.internal.change.persistence.AddendumStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<BedAllocationCommandApi> beds;

    AddendumClosing(AddendumStore addenda, AuditLogApi auditLog, ObjectProvider<BedAllocationCommandApi> beds) {
        this.addenda = addenda;
        this.auditLog = auditLog;
        this.beds = beds;
    }

    void close(long approvalRequestId, long residentId, Basis basis) {
        addenda.setPending(approvalRequestId, false);
        addenda.lockByRequest(approvalRequestId)
                .filter(addendum -> addendum.status() == Addendum.Status.PENDING_EFFECTIVE)
                .ifPresent(addendum -> {
                    addenda.markNotApplied(addendum.id());
                    if (addendum.changeTypes().contains(ChangeType.BED)) {
                        // Phân bổ theo lịch đặt lúc duyệt phải hủy cùng phụ lục (FR-045); thiếu bản cài thì không có gì
                        // để hủy
                        beds.ifAvailable(api -> api.cancelScheduledAllocation(
                                approvalRequestId, new Reason("Phụ lục không áp dụng (FR-045)")));
                    }
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
