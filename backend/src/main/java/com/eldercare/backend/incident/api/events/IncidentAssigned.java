package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Sự cố đã giao cho người xử lý (FR-045a, Q-339). */
public record IncidentAssigned(
        long incidentId,
        long assignedById,
        long assigneeId,
        String level,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public IncidentAssigned {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
