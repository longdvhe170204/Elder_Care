package com.cambae.backend.m03.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
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
