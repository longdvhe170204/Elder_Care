package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Sự kiện công khai theo hợp đồng 007 mục 1.12; nhóm nhận do publisher nghiệp vụ xác định. */
public record IsolationCreated(
        long zoneId,
        List<Long> floorAreaIds,
        List<Long> areaIds,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public IsolationCreated {
        notificationRequests = List.copyOf(notificationRequests);
        floorAreaIds = List.copyOf(floorAreaIds);
        areaIds = List.copyOf(areaIds);
    }
}
