package com.cambae.backend.m02.internal.contract.service;

import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ApprovalStatus;
import com.cambae.backend.m15.approval.api.RequestCancelled;
import com.cambae.backend.m15.approval.api.RequestDecided;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Mở khóa nội dung hợp đồng khi yêu cầu duyệt điều khoản khác chuẩn bị từ chối hoặc hủy (FR-025a; research T-04): hợp đồng
 * quay về Nháp sửa được, bản duyệt không còn. Chạy sau khi quyết định đã commit (một giao dịch riêng); yêu cầu được duyệt
 * thì {@link ContractTermsRequestHandler} mở khóa ngay trong lần áp dụng.
 */
@Component
class ContractTermsListeners {

    private final ContractStore store;
    private final ResidencyLock residencyLock;

    ContractTermsListeners(ContractStore store, ResidencyLock residencyLock) {
        this.store = store;
        this.residencyLock = residencyLock;
    }

    @ApplicationModuleListener
    void onDecided(RequestDecided event) {
        if (ApprovalStatus.REJECTED.equals(event.outcome())) {
            release(event.request());
        }
    }

    @ApplicationModuleListener
    void onCancelled(RequestCancelled event) {
        release(event.request());
    }

    private void release(ApprovalRequestView request) {
        if (!ContractTermsRequestHandler.TYPE.equals(request.type())) {
            return;
        }
        long contractId = ContractTermsRequestHandler.contractId(request.content());
        Contract contract = store.find(contractId).orElse(null);
        if (contract == null) {
            return;
        }
        residencyLock.lock(contract.residentId());
        store.releaseTerms(contractId, request.id());
    }
}
