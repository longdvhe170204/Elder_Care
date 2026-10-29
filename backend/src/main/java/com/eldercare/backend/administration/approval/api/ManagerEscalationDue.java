package com.eldercare.backend.administration.approval.api;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;

/**
 * Báo Quản lý viện ({@code YeuCauCanBaoQuanLyVien}): quá CFG-M15-06 mà người duyệt không phải Quản lý viện, hoặc tới
 * mốc nhắc mà không còn người duyệt (FR-031a, BR-M15-09). Yêu cầu vẫn ở Chờ duyệt.
 */
public record ManagerEscalationDue(ApprovalRequestView request, OverdueReminder.EscalationReason reason, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String text = reason == OverdueReminder.EscalationReason.NO_APPROVER_LEFT
                ? "Yêu cầu " + request.displayCode() + " chờ duyệt nhưng không còn người có quyền duyệt."
                : "Yêu cầu " + request.displayCode() + " vẫn chưa được duyệt sau thời hạn báo Quản lý viện.";
        return List.of(Notices.of(
                request,
                "REQUEST_ESCALATION",
                List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                text,
                at));
    }
}
