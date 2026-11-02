package com.eldercare.backend.billing.api.events;

import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Contracts 010 mục 1.6: nguồn phát trong lệnh; chuyển sau commit qua outbox. */
public record CostStatementFinalized(
        long statementId,
        CostStatementType statementType,
        long periodId,
        long residentId,
        long total,
        Instant finalizedAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public CostStatementFinalized {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
