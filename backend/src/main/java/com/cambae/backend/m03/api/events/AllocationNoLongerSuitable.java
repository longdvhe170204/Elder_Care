package com.cambae.backend.m03.api.events;

import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
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
