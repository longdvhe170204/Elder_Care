package com.eldercare.backend.incident.internal.alert.domain;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/** Thông báo thêm của cảnh báo đang mở (chi tiết nút hỗ trợ, FR-047i); 009 nhận qua {@link HasNotificationRequests}. */
public record AlertNoticeRequested(long alertId, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public AlertNoticeRequested {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
