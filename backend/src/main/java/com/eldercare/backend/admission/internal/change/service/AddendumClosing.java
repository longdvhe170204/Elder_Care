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
 * Đóng phụ lục khi yêu cầu của nó không còn được áp dụng (FR-042, FR-045, FR-043f; data-model mục 7): phụ lục còn chờ (Chờ
 * ký, Chờ hiệu lực) chuyển Không áp dụng ({@link #close}: yêu cầu Áp dụng không thành, bị thay thế, người cao tuổi sang
 * trạng thái cuối) hoặc Đã hủy ({@link #cancel}: yêu cầu bị hủy, Q-421); thuộc tính được nhả để yêu cầu khác dùng (FR-046),
 * giữ giường đặt lúc duyệt bị hủy. Chạy trong transaction của chỗ gọi.
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
        end(approvalRequestId, residentId, basis, false);
    }

    /** Yêu cầu bị hủy (Q-421): phụ lục còn chờ chuyển Đã hủy. */
    void cancel(long approvalRequestId, long residentId, Basis basis) {
        end(approvalRequestId, residentId, basis, true);
    }

    private void end(long approvalRequestId, long residentId, Basis basis, boolean cancelled) {
        addenda.setPending(approvalRequestId, false);
        addenda.lockByRequest(approvalRequestId).filter(Addendum::open).ifPresent(addendum -> {
            if (cancelled) {
                addenda.markCancelled(addendum.id());
            } else {
                addenda.markNotApplied(addendum.id());
            }
            if (addendum.changeTypes().contains(ChangeType.BED)
                    || addendum.changeTypes().contains(ChangeType.RESIDENCE_TYPE)) {
                // Phân bổ đặt lúc duyệt (chuyển theo lịch, hoặc phân bổ đầu tiên của người bán trú sang nội
                // trú) phải hủy cùng phụ lục (FR-045); 003 không có phân bổ đặt trước thì không làm gì
                beds.getObject()
                        .cancelScheduledAllocation(
                                approvalRequestId,
                                new Reason(
                                        cancelled
                                                ? "Yêu cầu điều chỉnh hợp đồng đã bị hủy"
                                                : "Phụ lục hợp đồng không áp dụng"));
            }
            Addendum.Status to = cancelled ? Addendum.Status.CANCELLED : Addendum.Status.NOT_APPLIED;
            auditLog.write(AuditLogEntry.builder()
                    .action(
                            cancelled
                                    ? ResidencyChangeHandler.ADDENDUM_CANCELLED
                                    : ResidencyChangeHandler.ADDENDUM_NOT_APPLIED)
                    .entity(ResidencyChangeHandler.ADDENDUM_ENTITY, addendum.id())
                    .resident(residentId)
                    .before(Map.of("status", addendum.status().name()))
                    .after(Map.of("status", to.name()))
                    .basis(basis)
                    .build());
        });
    }
}
