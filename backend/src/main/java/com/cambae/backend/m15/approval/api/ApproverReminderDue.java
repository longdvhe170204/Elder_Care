package com.cambae.backend.m15.approval.api;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;

/** Quá mốc nhắc CFG-M15-05 ({@code YeuCauCanNhacNguoiDuyet}, FR-031a); {@code approvers} đã lọc giới hạn vai trò. */
public record ApproverReminderDue(ApprovalRequestView request, List<Long> approvers, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                request,
                "REQUEST_REMINDER",
                List.of(new RecipientGroup.Approvers(request.type(), approvers)),
                "Yêu cầu " + request.displayCode() + " chờ duyệt đã quá thời hạn nhắc.",
                at));
    }
}
