package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Người phụ trách cảnh báo hoặc sự cố đã đổi sau bàn giao (FR-037). */
public record AssigneeChanged(
        String sourceType,
        long sourceId,
        long newAssigneeId,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public AssigneeChanged {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
