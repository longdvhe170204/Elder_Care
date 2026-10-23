package com.cambae.backend.m03.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
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
