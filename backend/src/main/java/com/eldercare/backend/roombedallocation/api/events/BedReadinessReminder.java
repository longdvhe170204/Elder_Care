package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import java.time.Instant;
import java.util.List;

/** BedReadinessReminder: mức LOW, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record BedReadinessReminder(long bedId, Instant deadline, BedNotificationContext notification)
        implements HasNotificationRequests {
    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "BED_READINESS_REMINDER",
                bedId,
                null,
                "BED",
                NotificationLevel.LOW,
                InformationType.GENERAL,
                deadline.toString(),
                notification);
    }
}
