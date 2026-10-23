package com.cambae.backend.m09.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Sự kiện 008 mục 1.6; dịch vụ nguồn phát trong transaction, Modulith chuyển sau commit. */
public record SchedulePublished(
        long scheduleId, List<Long> staffIds, Instant occurredAt, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public SchedulePublished {
        staffIds = List.copyOf(staffIds);
        notificationRequests = List.copyOf(notificationRequests);
    }

    public String eventKey() {
        return "SCHEDULE_PUBLISHED:" + scheduleId + ":" + occurredAt;
    }
}
