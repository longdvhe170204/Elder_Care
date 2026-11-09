package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Sự cố quá hạn tiếp nhận đã lên cấp leo thang, trạng thái giữ Mới (FR-044, research S-11). */
public record IncidentEscalated(
        long incidentId,
        int escalationLevel,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public IncidentEscalated {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
