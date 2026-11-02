package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Cảnh báo cùng nguồn lặp lại khi cảnh báo trước còn mở (FR-038a). */
public record AlertRepeated(
        long alertId, int count, String eventKey, Instant occurredAt, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public AlertRepeated {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
