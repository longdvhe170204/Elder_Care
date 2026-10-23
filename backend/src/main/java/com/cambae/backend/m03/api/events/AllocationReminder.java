package com.cambae.backend.m03.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
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
