package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import java.util.List;

/** FutureAllocationCancelledForDamagedBed: mức MEDIUM, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record FutureAllocationCancelledForDamagedBed(
        long bedId, List<Long> residentIds, BedNotificationContext notification) implements HasNotificationRequests {
    public FutureAllocationCancelledForDamagedBed {
        residentIds = List.copyOf(residentIds);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "FUTURE_ALLOCATION_CANCELLED_FOR_DAMAGED_BED",
                bedId,
                null,
                "BED",
                NotificationLevel.MEDIUM,
                InformationType.GENERAL,
                "",
                notification);
    }
}
