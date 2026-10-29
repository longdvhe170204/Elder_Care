package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import java.time.Instant;
import java.util.List;

/** FutureAllocationPastStart: mức MEDIUM, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record FutureAllocationPastStart(long allocationId, Instant expectedStart, BedNotificationContext notification)
        implements HasNotificationRequests {
    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "FUTURE_ALLOCATION_PAST_START",
                allocationId,
                null,
                "BED_ALLOCATION",
                NotificationLevel.MEDIUM,
                InformationType.GENERAL,
                expectedStart.toString(),
                notification);
    }
}
