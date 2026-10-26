package com.cambae.backend.m09.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Sự kiện 008 mục 1.6; dịch vụ nguồn phát trong transaction, Modulith chuyển sau commit. */
public record AssignmentsEndedByCredential(
        long staffId,
        List<Long> shiftIds,
        List<Long> residentIds,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public AssignmentsEndedByCredential {
        shiftIds = List.copyOf(shiftIds);
        residentIds = List.copyOf(residentIds);
        notificationRequests = List.copyOf(notificationRequests);
    }

    public String eventKey() {
        return "ASSIGNMENTS_ENDED_BY_CREDENTIAL:" + staffId + ":" + occurredAt;
    }
}
