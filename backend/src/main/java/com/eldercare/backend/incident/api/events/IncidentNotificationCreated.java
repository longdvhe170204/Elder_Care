package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Yêu cầu thông báo khi lập sự cố (FR-043a). */
public record IncidentNotificationCreated(
        long incidentId,
        String level,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public IncidentNotificationCreated {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
