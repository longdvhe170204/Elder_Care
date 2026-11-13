package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.administration.approval.api.events.RequestCancelled;
import com.eldercare.backend.administration.approval.api.events.RequestDecided;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Phụ lục chuyển Không áp dụng khi yêu cầu Đã duyệt của nó bị hủy (FR-042; data-model mục 7), và thuộc tính được nhả để yêu
 * cầu khác dùng (FR-046). Hủy chạy đồng bộ trong transaction của lệnh hủy (người hủy là người thực hiện trong nhật ký; listener
 * sau commit không có người dùng hiện tại, quickstart 004 Q15); từ chối cũng đồng bộ (quickstart 003 Q15). Yêu cầu bị hủy khi còn Nháp, Chờ duyệt
 * không có phụ lục nên chỉ nhả chỉ mục thuộc tính. Yêu cầu Áp dụng không thành không nghe sự kiện {@code RequestApplyFailed}
 * (chỉ M13 được nghe sự kiện có thông báo, {@code M13ArchTest}): {@link ResidencyChangeHandler#checkApplyConditions} đóng
 * phụ lục ngay trong transaction áp dụng, cùng lúc yêu cầu chuyển Áp dụng không thành.
 */
@Component
class ResidencyChangeListeners {

    private final AddendumClosing closing;
    private final ResidencyLock residencyLock;
    private final ResidencyChangeHandler handler;

    ResidencyChangeListeners(AddendumClosing closing, ResidencyLock residencyLock, ResidencyChangeHandler handler) {
        this.closing = closing;
        this.residencyLock = residencyLock;
        this.handler = handler;
    }

    /**
     * 003 FR-030: yêu cầu sau chuyển gấp bị từ chối. Đồng bộ trong transaction của lệnh từ chối (người duyệt là người thực
     * hiện; listener sau commit không có người dùng hiện tại, quickstart 003 Q15).
     */
    @EventListener
    void onDecided(RequestDecided event) {
        ApprovalRequestView request = event.request();
        if (ResidencyChangeHandler.TYPE.equals(request.type()) && ApprovalStatus.REJECTED.equals(event.outcome())) {
            residencyLock.lock(request.target().residentId());
            handler.rejected(request);
        }
    }

    @EventListener
    void onCancelled(RequestCancelled event) {
        ApprovalRequestView request = event.request();
        if (!ResidencyChangeHandler.TYPE.equals(request.type())) {
            return;
        }
        residencyLock.lock(request.target().residentId());
        closing.close(request.id(), request.target().residentId(), new Basis(null, request.id(), null, null));
        handler.withdrawn(request);
    }
}
