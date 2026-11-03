package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Cần xác nhận lại nguyện vọng khi nguy kịch (FR-050b). */
public record WishReconfirmationRequested(
        long alertId,
        long residentId,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public WishReconfirmationRequested {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
