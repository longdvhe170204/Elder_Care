package com.eldercare.backend.administration.approval.api;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestDetails;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.CreateRequest;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.UnmetCondition;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

/** Yêu cầu phê duyệt cho module khác (contracts/module-interfaces.md mục 1.5; FR-031 → FR-041). */
public interface ApprovalRequestApi {

    /** Lập yêu cầu kèm lệnh của một actor ở module khác; người lập là actor đó (FR-010a, Q-307). */
    ApprovalRequestView create(CreateRequest request);

    Optional<ApprovalRequestView> find(long id);

    /** Chi tiết kèm lệnh người dùng hiện tại được phép; không xem được thì 404 (FR-042). */
    ApprovalRequestDetails detail(long id);

    /**
     * Sửa nội dung yêu cầu còn Nháp qua lệnh của module sở hữu (Propagation.MANDATORY; bổ sung theo plan 004 T043, {@code PUT
     * /residency-change-requests/{id}}): cùng kiểm và nhật ký như {@code edit-content} của HTTP, người sửa theo
     * {@code RequestTypeHandler.mayActOnDraft}; {@code ifMatch} là ETag của yêu cầu, ngày hiệu lực null thì giữ ngày cũ.
     */
    ApprovalRequestView editDraft(
            long id, String ifMatch, JsonNode content, LocalDate requestedEffectiveDate, Reason reason);

    /** Kết quả áp dụng (FR-039). */
    Optional<ResultRef> applicationResult(long requestId);

    /** Chuyển mở rộng do hệ thống thực hiện kèm lệnh của actor ở module khác (Propagation.MANDATORY; bổ sung theo plan 001). */
    void applyExtendedTransition(long requestId, String transitionCode, Basis basis);

    /**
     * Đóng yêu cầu khi người cao tuổi sang trạng thái cuối (Propagation.MANDATORY; bổ sung theo plan 004): Nháp, Chờ duyệt
     * thành Hủy; Đã duyệt thành Áp dụng không thành; trạng thái cuối thì không làm gì.
     */
    void closeForFinalState(long requestId, Basis basis);

    /**
     * Yêu cầu Đã duyệt chưa áp dụng thành Áp dụng không thành vì điều kiện ngoài yêu cầu không còn đạt, kèm lệnh của actor ở
     * module khác (Propagation.MANDATORY; bổ sung theo 003 FR-013a: giường hỏng trước ngày chuyển giường theo lịch). Ghi
     * điều kiện vào lý do đóng, nhật ký kèm căn cứ, phát {@code RequestApplyFailed}; trạng thái khác thì 409
     * {@code INVALID_STATE}.
     */
    void failApplication(long requestId, List<UnmetCondition> unmet, Basis basis);
}
