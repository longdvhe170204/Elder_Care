package com.cambae.backend.m03.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
import java.util.List;

/** FormerOccupantListed: mức MEDIUM, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record FormerOccupantListed(long bedId, long residentId, BedNotificationContext notification)
        implements HasNotificationRequests {
    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "FORMER_OCCUPANT_LISTED",
                bedId,
                residentId,
                "BED",
                NotificationLevel.MEDIUM,
                InformationType.HEALTH,
                "",
                notification);
    }
}
