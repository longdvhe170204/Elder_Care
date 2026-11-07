package com.eldercare.backend.roombedallocation.internal.asset.domain;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/**
 * 019 FR-015, FR-021, Q-245, Q-256: hư hỏng giường có người, trạng thái không đổi (nhắc Trưởng tầng chuyển người, hoặc
 * Khẩn cấp với mức mất an toàn). Sự kiện nội bộ của M03.
 */
public record BedDamageNotice(long assetId, long bedId, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public BedDamageNotice {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
