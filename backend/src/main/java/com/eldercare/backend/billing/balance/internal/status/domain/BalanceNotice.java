package com.eldercare.backend.billing.balance.internal.status.domain;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/** Thông báo số dư (FR-018 → FR-020, FR-030) gửi qua 009. */
public record BalanceNotice(List<NotificationRequest> notificationRequests) implements HasNotificationRequests {

    public BalanceNotice {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
