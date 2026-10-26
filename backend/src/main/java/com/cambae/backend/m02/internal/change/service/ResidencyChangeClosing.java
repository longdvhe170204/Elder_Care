package com.cambae.backend.m02.internal.change.service;

import com.cambae.backend.m02.internal.change.persistence.AddendumStore;
import com.cambae.backend.m15.approval.api.ApprovalRequestApi;
import com.cambae.backend.m15.auditlog.api.Basis;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đóng yêu cầu thay đổi lưu trú của người cao tuổi khi sang trạng thái cuối (FR-065, FR-070; research T-06): mỗi yêu cầu đã
 * ghi nhận ở {@code change_request_attribute} được đóng bằng {@code closeForFinalState} của 000 (Nháp, Chờ duyệt thành Hủy; Đã
 * duyệt thành Áp dụng không thành; trạng thái cuối giữ nguyên), rồi phụ lục Chờ hiệu lực của nó chuyển Không áp dụng. Do
 * {@code M02StatusParticipant} gọi trong transaction của lệnh trạng thái.
 */
@Component
public class ResidencyChangeClosing {

    private final AddendumStore addenda;
    private final AddendumClosing closing;
    private final ObjectProvider<ApprovalRequestApi> approvals;

    ResidencyChangeClosing(
            AddendumStore addenda, AddendumClosing closing, ObjectProvider<ApprovalRequestApi> approvals) {
        this.addenda = addenda;
        this.closing = closing;
        this.approvals = approvals;
    }

    /** Đóng mọi yêu cầu thay đổi lưu trú đã ghi nhận của người cao tuổi; căn cứ là quy tắc {@code ruleCode}. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void closeAll(long residentId, String ruleCode) {
        Basis basis = new Basis(ruleCode, null, null, null);
        for (long requestId : addenda.requestsOf(residentId)) {
            approvals.getObject().closeForFinalState(requestId, basis);
            closing.close(requestId, residentId, new Basis(ruleCode, requestId, null, null));
        }
    }
}
