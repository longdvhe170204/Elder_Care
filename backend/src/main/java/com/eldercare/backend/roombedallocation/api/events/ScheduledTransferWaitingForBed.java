package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import java.util.List;

/** ScheduledTransferWaitingForBed: mức MEDIUM, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record ScheduledTransferWaitingForBed(long allocationId, long bedId, BedNotificationContext notification)
        implements HasNotificationRequests {
    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "SCHEDULED_TRANSFER_WAITING_FOR_BED",
                allocationId,
                null,
                "BED_ALLOCATION",
                NotificationLevel.MEDIUM,
                InformationType.GENERAL,
                "",
                notification);
    }
}
