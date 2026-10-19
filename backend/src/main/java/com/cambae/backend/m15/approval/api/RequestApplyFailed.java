package com.cambae.backend.m15.approval.api;

import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;

/**
 * Áp dụng không thành vì điều kiện không thỏa ({@code YeuCauApDungKhongThanh}, FR-038): báo người duyệt đã quyết định;
 * phần báo người yêu cầu chờ 009 bổ sung nhóm người nhận theo tài khoản.
 */
public record RequestApplyFailed(ApprovalRequestView request, List<UnmetCondition> conditions, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        if (request.decidedBy() == null) {
            return List.of();
        }
        return List.of(Notices.of(
                request,
                "REQUEST_APPLY_FAILED",
                List.of(new RecipientGroup.Approvers(request.type(), List.of(request.decidedBy()))),
                "Yêu cầu " + request.displayCode() + " áp dụng không thành vì điều kiện không còn thỏa.",
                at));
    }
}
