package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Cần lựa chọn cho tạm vắng về nhà khi xử lý nguy kịch (FR-050d). */
public record TemporaryHomeLeaveRequested(
        long residentId,
        long alertId,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public TemporaryHomeLeaveRequested {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
