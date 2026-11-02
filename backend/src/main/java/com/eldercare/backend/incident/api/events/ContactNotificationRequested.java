package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Thông báo cho người trong danh sách tiếp xúc; yêu cầu người thân mang mã Q-354 (FR-062). */
public record ContactNotificationRequested(
        long contactListId, String eventKey, Instant occurredAt, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public ContactNotificationRequested {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
