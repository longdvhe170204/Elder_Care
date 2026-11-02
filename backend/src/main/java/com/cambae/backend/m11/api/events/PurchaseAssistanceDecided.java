package com.cambae.backend.m11.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.m11.api.dto.*;
import java.util.List;

/** Contracts 010 mục 1.6: nguồn phát trong lệnh; chuyển sau commit qua outbox. */
public record PurchaseAssistanceDecided(
        long requestId,
        long residentId,
        long estimatedAmount,
        PurchaseAssistanceStatus status,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public PurchaseAssistanceDecided {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
