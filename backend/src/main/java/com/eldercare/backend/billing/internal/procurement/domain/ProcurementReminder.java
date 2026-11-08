package com.eldercare.backend.billing.internal.procurement.domain;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/** 019 BR-M11-20: nhắc yêu cầu mua sắm quá hạn ghi đã mua hoặc đối chiếu. Sự kiện nội bộ của M11. */
public record ProcurementReminder(long requestId, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public ProcurementReminder {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
