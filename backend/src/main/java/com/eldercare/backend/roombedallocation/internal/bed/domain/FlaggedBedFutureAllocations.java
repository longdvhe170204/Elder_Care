package com.eldercare.backend.roombedallocation.internal.bed.domain;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/** 003 FR-013b: giường mang dấu "chờ chuyển người" có phân bổ tương lai được giữ; báo Hành chính, Trưởng tầng. */
public record FlaggedBedFutureAllocations(
        long bedId, List<Long> residentIds, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public FlaggedBedFutureAllocations {
        residentIds = List.copyOf(residentIds);
        notificationRequests = List.copyOf(notificationRequests);
    }
}
