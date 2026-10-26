package com.cambae.backend.m02.internal.change.service;

import com.cambae.backend.m01.api.spi.CareLevelChangeRequestApi;
import com.cambae.backend.m02.internal.change.domain.ChangeType;
import com.cambae.backend.m02.internal.change.persistence.AddendumStore;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.approval.api.ApprovalRequestApi;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ApprovalStatus;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yêu cầu đổi mức chăm sóc cho 001 (FR-036, FR-036a; SPI {@link CareLevelChangeRequestApi} do 001 khai báo, M02 cài): yêu
 * cầu thay đổi lưu trú có loại đổi mức mà chưa kết thúc (Nháp, Chờ duyệt, Đã duyệt chờ hiệu lực) bị thay thế khi một lần
 * đánh giá được xác nhận. Yêu cầu chuyển {@code SUPERSEDED} qua chuyển mở rộng của 000, phụ lục của yêu cầu Đã duyệt chuyển
 * Không áp dụng và thuộc tính được nhả, cùng transaction với lệnh xác nhận đánh giá. Yêu cầu có thêm loại khác ngoài đổi mức
 * cũng bị thay thế nguyên yêu cầu (không tách được một phần); người lập lại phần còn lại.
 */
@Component
class CareLevelChangeRequestHandler implements CareLevelChangeRequestApi {

    private static final Set<String> OPEN =
            Set.of(ApprovalStatus.DRAFT, ApprovalStatus.PENDING_APPROVAL, ApprovalStatus.APPROVED);

    private final AddendumStore addenda;
    private final AddendumClosing closing;
    private final ResidencyLock residencyLock;
    private final ObjectProvider<ApprovalRequestApi> approvals;

    CareLevelChangeRequestHandler(
            AddendumStore addenda,
            AddendumClosing closing,
            ResidencyLock residencyLock,
            ObjectProvider<ApprovalRequestApi> approvals) {
        this.addenda = addenda;
        this.closing = closing;
        this.residencyLock = residencyLock;
        this.approvals = approvals;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> supersedeByAssessment(long residentId, long assessmentId) {
        List<Long> open = openRequests(residentId);
        if (open.isEmpty()) {
            return List.of();
        }
        residencyLock.lock(residentId);
        List<Long> superseded = new ArrayList<>();
        for (long requestId : open) {
            approvals
                    .getObject()
                    .applyExtendedTransition(
                            requestId,
                            ResidencyChangeHandler.SUPERSEDE,
                            new Basis("BR-M01-03", requestId, assessmentId, null));
            closing.close(requestId, residentId, new Basis("BR-M01-03", requestId, assessmentId, null));
            superseded.add(requestId);
        }
        return superseded;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOpenRequest(long residentId) {
        return !openRequests(residentId).isEmpty();
    }

    private List<Long> openRequests(long residentId) {
        List<Long> open = new ArrayList<>();
        for (long requestId : addenda.requestsOf(residentId, ChangeType.CARE_LEVEL)) {
            approvals
                    .getObject()
                    .find(requestId)
                    .map(ApprovalRequestView::status)
                    .filter(OPEN::contains)
                    .ifPresent(status -> open.add(requestId));
        }
        return open;
    }
}
