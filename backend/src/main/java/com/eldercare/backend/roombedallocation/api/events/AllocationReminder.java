package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import java.util.List;

/** AllocationReminder: mức LOW, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record AllocationReminder(String flagType, long targetId, int reminderNo, BedNotificationContext notification)
        implements HasNotificationRequests {
    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "ALLOCATION_REMINDER",
                targetId,
                null,
                "BED_ALLOCATION",
                NotificationLevel.LOW,
                InformationType.GENERAL,
                flagType + ":" + reminderNo,
                notification);
    }
}
