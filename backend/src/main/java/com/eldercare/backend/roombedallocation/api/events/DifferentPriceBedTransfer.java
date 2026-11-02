package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import java.util.List;

/** DifferentPriceBedTransfer: mức MEDIUM, nhóm người nhận do lệnh nguồn đặt theo G-12. */
public record DifferentPriceBedTransfer(
        long allocationId, long residentId, long causedBy, BedNotificationContext notification)
        implements HasNotificationRequests {
    @Override
    public List<NotificationRequest> notificationRequests() {
        return BedEventNotifications.create(
                "DIFFERENT_PRICE_BED_TRANSFER",
                allocationId,
                residentId,
                "BED_ALLOCATION",
                NotificationLevel.MEDIUM,
                InformationType.GENERAL,
                "",
                notification);
    }
}
