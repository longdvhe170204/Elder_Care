package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Cần lập công việc theo dõi sau ngã (FR-051, FR-052). */
public record PostFallFollowUpRequested(
        long residentId,
        long incidentId,
        Instant startsAt,
        Instant endsAt,
        Duration measurementInterval,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public PostFallFollowUpRequested {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
