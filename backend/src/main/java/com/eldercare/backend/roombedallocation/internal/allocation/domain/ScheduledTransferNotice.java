package com.eldercare.backend.roombedallocation.internal.allocation.domain;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/**
 * 003 FR-029, FR-030: thông báo của chuyển theo lịch (không còn đạt điều kiện khi giường sẵn sàng) và kết quả yêu cầu
 * sau chuyển gấp. Sự kiện nội bộ của M03.
 */
public record ScheduledTransferNotice(long allocationId, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public ScheduledTransferNotice {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
