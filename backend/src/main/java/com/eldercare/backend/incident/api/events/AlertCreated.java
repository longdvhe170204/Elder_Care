package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Sự kiện công khai theo hợp đồng 007 mục 1.12; nhóm nhận do publisher nghiệp vụ xác định. */
public record AlertCreated(
        long alertId,
        String level,
        boolean noOnDutyDoctor,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public AlertCreated {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
