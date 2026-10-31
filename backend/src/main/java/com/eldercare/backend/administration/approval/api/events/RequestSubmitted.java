package com.eldercare.backend.administration.approval.api.events;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;

/** Đã gửi duyệt ({@code YeuCauDaGuiDuyet}); {@code approvers} đã lọc theo giới hạn vai trò duyệt (contracts mục 1.5). */
public record RequestSubmitted(ApprovalRequestView request, List<Long> approvers, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        if (approvers.isEmpty()) {
            return List.of();
        }
        return List.of(Notices.of(
                request,
                "REQUEST_SUBMITTED",
                List.of(new RecipientGroup.Approvers(request.type(), approvers)),
                "Yêu cầu " + request.displayCode() + " đang chờ duyệt.",
                at));
    }
}
