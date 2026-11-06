package com.eldercare.backend.billing.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/** FR-038a: 017/016 đọc lại số liệu của người cao tuổi và kỳ; không mang danh sách khoản. */
public record CostItemChanged(long residentId, long periodId, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public CostItemChanged(long residentId, long periodId) {
        this(residentId, periodId, List.of());
    }

    public CostItemChanged {
        // Bản outbox trước khi thêm thông báo không có trường này.
        notificationRequests = notificationRequests == null ? List.of() : List.copyOf(notificationRequests);
    }
}
