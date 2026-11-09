package com.eldercare.backend.staffing.internal.swap.service;

import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.administration.approval.api.events.RequestCancelled;
import com.eldercare.backend.administration.approval.api.events.RequestDecided;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Nghe lệnh Hủy, Từ chối của khung phê duyệt chung (000) cho yêu cầu của 015, để chi tiết và khóa (ca, nhân viên) khớp
 * trạng thái cuối (data-model mục 5: khóa xóa khi yêu cầu vào trạng thái cuối). Chạy trong transaction của lệnh gọi.
 */
@Component
class SwapRequestEvents {

    private final SwapService swaps;
    private final LeaveService leaves;

    SwapRequestEvents(SwapService swaps, LeaveService leaves) {
        this.swaps = swaps;
        this.leaves = leaves;
    }

    @EventListener
    void onCancelled(RequestCancelled event) {
        route(event.request().type(), event.request().id(), ApprovalStatus.CANCELLED);
    }

    @EventListener
    void onDecided(RequestDecided event) {
        if (ApprovalStatus.REJECTED.equals(event.outcome())) {
            route(event.request().type(), event.request().id(), ApprovalStatus.REJECTED);
        }
    }

    private void route(String type, long id, String status) {
        if (SwapHandler.TYPE.equals(type)) {
            swaps.syncClosedByGenericCommand(id, status);
        } else if (LeaveHandler.TYPE.equals(type)) {
            leaves.syncClosedByGenericCommand(id, status);
        }
    }
}
