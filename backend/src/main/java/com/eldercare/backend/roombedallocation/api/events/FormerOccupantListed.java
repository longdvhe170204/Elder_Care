package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
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
