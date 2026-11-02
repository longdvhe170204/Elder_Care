package com.cambae.backend.m11.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.m11.api.dto.*;
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
