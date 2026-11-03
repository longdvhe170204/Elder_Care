package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Nguồn của cảnh báo đã được đính chính (FR-019). */
public record AlertSourceCorrected(
        long alertId, String eventKey, Instant occurredAt, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public AlertSourceCorrected {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
