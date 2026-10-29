package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import java.util.List;

/** AllocationNoLongerSuitable: mức MEDIUM, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record AllocationNoLongerSuitable(
        long allocationId, List<UnmetCondition> conditions, BedNotificationContext notification)
        implements HasNotificationRequests {
    public AllocationNoLongerSuitable {
        conditions = List.copyOf(conditions);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "ALLOCATION_NO_LONGER_SUITABLE",
                allocationId,
                null,
                "BED_ALLOCATION",
                NotificationLevel.MEDIUM,
                InformationType.GENERAL,
                "",
                notification);
    }
}
