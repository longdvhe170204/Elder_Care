package com.cambae.backend.m15.approval.api;

import com.cambae.backend.m15.auditlog.api.Basis;
import java.util.Optional;

/** Yêu cầu phê duyệt cho module khác (contracts/module-interfaces.md mục 1.5; FR-031 → FR-041). */
public interface ApprovalRequestApi {

    /** Lập yêu cầu kèm lệnh của một actor ở module khác; người lập là actor đó (FR-010a, Q-307). */
    ApprovalRequestView create(CreateRequest request);

    Optional<ApprovalRequestView> find(long id);

    /** Kết quả áp dụng (FR-039). */
    Optional<ResultRef> applicationResult(long requestId);

    /** Chuyển mở rộng do hệ thống thực hiện kèm lệnh của actor ở module khác (Propagation.MANDATORY; bổ sung theo plan 001). */
    void applyExtendedTransition(long requestId, String transitionCode, Basis basis);

    /**
     * Đóng yêu cầu khi người cao tuổi sang trạng thái cuối (Propagation.MANDATORY; bổ sung theo plan 004): Nháp, Chờ duyệt
     * thành Hủy; Đã duyệt thành Áp dụng không thành; trạng thái cuối thì không làm gì.
     */
    void closeForFinalState(long requestId, Basis basis);
}
