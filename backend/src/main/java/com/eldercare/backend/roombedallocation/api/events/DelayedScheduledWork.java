package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import java.time.Instant;
import java.util.List;

/** DelayedScheduledWork: mức MEDIUM, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record DelayedScheduledWork(
        String job, long targetId, Instant plannedAt, Instant actualAt, BedNotificationContext notification)
        implements HasNotificationRequests {
    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "DELAYED_SCHEDULED_WORK",
                targetId,
                null,
                "BED",
                NotificationLevel.MEDIUM,
                InformationType.GENERAL,
                job + ":" + plannedAt,
                notification);
    }
}
