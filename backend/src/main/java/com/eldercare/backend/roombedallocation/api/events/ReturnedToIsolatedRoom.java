package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import java.util.List;

/** ReturnedToIsolatedRoom: mức URGENT, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record ReturnedToIsolatedRoom(
        long allocationId, long residentId, long roomId, BedNotificationContext notification)
        implements HasNotificationRequests {
    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "RETURNED_TO_ISOLATED_ROOM",
                allocationId,
                residentId,
                "BED_ALLOCATION",
                NotificationLevel.URGENT,
                InformationType.HEALTH,
                "",
                notification);
    }
}
