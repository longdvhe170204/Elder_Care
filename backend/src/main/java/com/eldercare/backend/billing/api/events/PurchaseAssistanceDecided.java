package com.eldercare.backend.billing.api.events;

import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
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
