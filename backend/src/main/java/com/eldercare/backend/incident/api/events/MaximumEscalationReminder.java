package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/**
 * Nhắc cảnh báo hoặc sự cố đã ở cấp leo thang tối đa (FR-034, FR-044); {@code sourceType} {@code M05.ALERT} hoặc
 * {@code M05.INCIDENT}.
 */
public record MaximumEscalationReminder(
        String sourceType,
        long sourceId,
        int sequence,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public MaximumEscalationReminder {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
