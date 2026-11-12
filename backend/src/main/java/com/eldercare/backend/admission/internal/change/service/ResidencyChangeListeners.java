package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.administration.approval.api.events.RequestCancelled;
import com.eldercare.backend.administration.approval.api.events.RequestDecided;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Phụ lục chuyển Không áp dụng khi yêu cầu Đã duyệt của nó bị hủy (FR-042; data-model mục 7), và thuộc tính được nhả để yêu
 * cầu khác dùng (FR-046). Chạy sau khi quyết định đã commit (một giao dịch riêng). Yêu cầu bị hủy khi còn Nháp, Chờ duyệt
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

    /** 003 FR-030: yêu cầu sau chuyển gấp bị từ chối (transaction riêng sau khi quyết định đã commit). */
    @ApplicationModuleListener
    void onDecided(RequestDecided event) {
        ApprovalRequestView request = event.request();
        if (ResidencyChangeHandler.TYPE.equals(request.type()) && ApprovalStatus.REJECTED.equals(event.outcome())) {
            residencyLock.lock(request.target().residentId());
            handler.rejected(request);
        }
    }

    @ApplicationModuleListener
    void onCancelled(RequestCancelled event) {
        ApprovalRequestView request = event.request();
        if (!ResidencyChangeHandler.TYPE.equals(request.type())) {
            return;
        }
        residencyLock.lock(request.target().residentId());
        closing.close(request.id(), request.target().residentId(), new Basis(null, request.id(), null, null));
    }
}
