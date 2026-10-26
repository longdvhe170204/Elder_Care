package com.cambae.backend.m09.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Sự kiện 008 mục 1.6; dịch vụ nguồn phát trong transaction, Modulith chuyển sau commit. */
public record ResidentsWithoutAssignedStaff(
        long shiftId, List<Long> residentIds, Instant occurredAt, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public ResidentsWithoutAssignedStaff {
        residentIds = List.copyOf(residentIds);
        notificationRequests = List.copyOf(notificationRequests);
    }

    public String eventKey() {
        return "RESIDENTS_WITHOUT_ASSIGNED_STAFF:" + shiftId + ":" + occurredAt;
    }
}
