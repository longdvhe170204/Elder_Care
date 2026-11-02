package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Cần lập công việc đo nhiệt độ cho người tiếp xúc (FR-062). */
public record ContactTemperatureCheckRequested(
        long residentId,
        long contactPersonId,
        Instant from,
        Instant to,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public ContactTemperatureCheckRequested {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
