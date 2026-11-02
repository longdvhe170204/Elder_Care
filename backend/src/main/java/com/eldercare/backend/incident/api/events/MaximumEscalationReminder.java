package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Nhắc cảnh báo đã ở cấp leo thang tối đa (FR-034). */
public record MaximumEscalationReminder(
        long alertId, int sequence, String eventKey, Instant occurredAt, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public MaximumEscalationReminder {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
