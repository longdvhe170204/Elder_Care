package com.cambae.backend.m03.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
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
