package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Cần lập danh sách tiếp xúc khi ghi nhận nghi lây nhiễm (FR-060). */
public record ContactListRequested(
        long incidentId,
        long residentId,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public ContactListRequested {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
