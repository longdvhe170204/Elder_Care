package com.eldercare.backend.staffing.internal.swap.domain;

import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import java.util.Set;

/**
 * Trạng thái yêu cầu đổi ca và nghỉ đột xuất (data-model mục 4, 5): trạng thái chung của 000 cộng trạng thái mở rộng khai
 * qua {@code RequestTypeHandler.extendedTransitions}. {@code PENDING_APPROVAL} là "Chờ duyệt", {@code APPLIED} là "Đã áp
 * dụng", {@code REJECTED} là "Từ chối", {@code CANCELLED} là "Đã hủy".
 */
public final class SwapStatus {

    public static final String AWAITING_RECIPIENT = "AWAITING_RECIPIENT";
    public static final String RECIPIENT_DECLINED = "RECIPIENT_DECLINED";
    public static final String PARTIALLY_APPLIED = "PARTIALLY_APPLIED";
    public static final String EXPIRED = "EXPIRED";
    public static final String PENDING_APPROVAL = ApprovalStatus.PENDING_APPROVAL;
    public static final String APPLIED = ApprovalStatus.APPLIED;
    public static final String REJECTED = ApprovalStatus.REJECTED;
    public static final String CANCELLED = ApprovalStatus.CANCELLED;

    /** Còn mở: giữ khóa ca và nhân viên. */
    public static final Set<String> OPEN = Set.of(AWAITING_RECIPIENT, PENDING_APPROVAL);

    public static boolean isOpen(String status) {
        return OPEN.contains(status);
    }

    private SwapStatus() {}
}
