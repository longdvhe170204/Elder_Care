package com.cambae.backend.m15.approval.api;

import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Áp dụng không thành vì điều kiện không thỏa ({@code YeuCauApDungKhongThanh}, FR-038): báo người duyệt đã quyết định và
 * người yêu cầu. {@code requesterStaffId} là hồ sơ nhân viên của người yêu cầu (nhóm {@code Staff} của 009), {@code null}
 * khi tài khoản không gắn hồ sơ nhân viên.
 */
public record RequestApplyFailed(
        ApprovalRequestView request, List<UnmetCondition> conditions, Instant at, Long requesterStaffId)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<RecipientGroup> recipients = new ArrayList<>();
        if (request.decidedBy() != null) {
            recipients.add(new RecipientGroup.Approvers(request.type(), List.of(request.decidedBy())));
        }
        if (requesterStaffId != null) {
            recipients.add(new RecipientGroup.Staff(requesterStaffId, "REQUESTER"));
        }
        if (recipients.isEmpty()) {
            return List.of();
        }
        return List.of(Notices.of(
                request,
                "REQUEST_APPLY_FAILED",
                List.copyOf(recipients),
                "Yêu cầu " + request.displayCode() + " áp dụng không thành vì điều kiện không còn thỏa.",
                at));
    }
}
