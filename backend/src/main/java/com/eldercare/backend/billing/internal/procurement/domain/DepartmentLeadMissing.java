package com.eldercare.backend.billing.internal.procurement.domain;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/**
 * 019 FR-008, FR-021 (Q-292): lệnh của Phụ trách bộ phận bị chặn vì bộ phận chưa có Phụ trách; nhắc Quản lý viện chỉ định.
 * Phát trong transaction riêng để nhắc vẫn gửi dù lệnh bị chặn. Không phải hợp đồng công khai của M11.
 */
public record DepartmentLeadMissing(long departmentId, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public DepartmentLeadMissing {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
