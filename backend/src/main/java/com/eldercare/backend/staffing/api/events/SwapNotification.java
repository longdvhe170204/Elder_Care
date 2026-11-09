package com.eldercare.backend.staffing.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/**
 * Thông báo của yêu cầu đổi ca và nghỉ đột xuất (015 FR-044; contracts/module-interfaces.md mục 2.3). Nội dung không nêu
 * lý do (FR-045).
 */
public record SwapNotification(List<NotificationRequest> notificationRequests) implements HasNotificationRequests {

    public SwapNotification {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
