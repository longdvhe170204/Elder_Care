package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Danh sách tiếp xúc cần được xem lại (FR-061). */
public record ContactListReviewRequested(
        long contactListId, String eventKey, Instant occurredAt, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public ContactListReviewRequested {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
