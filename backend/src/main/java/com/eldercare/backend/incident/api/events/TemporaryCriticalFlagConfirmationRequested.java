package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Cần xác nhận dấu nguy kịch tạm thời (FR-050c). */
public record TemporaryCriticalFlagConfirmationRequested(
        long flagId, String eventKey, Instant occurredAt, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public TemporaryCriticalFlagConfirmationRequested {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
