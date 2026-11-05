package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Nhắc sự cố đã leo thang tối đa mà chưa có người tiếp nhận (FR-044 theo FR-034). */
public record IncidentMaximumEscalationReminder(
        long incidentId,
        int sequence,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public IncidentMaximumEscalationReminder {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
