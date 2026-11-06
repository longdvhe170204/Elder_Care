package com.eldercare.backend.billing.internal.period.domain;

import com.eldercare.backend.common.notification.*;
import java.util.List;

/** FR-044: phong bì nội bộ cho các lần nhắc; sự kiện chốt công khai giữ nguyên hợp đồng. */
public record CostNotificationEvent(List<NotificationRequest> notificationRequests) implements HasNotificationRequests {
    public CostNotificationEvent {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
