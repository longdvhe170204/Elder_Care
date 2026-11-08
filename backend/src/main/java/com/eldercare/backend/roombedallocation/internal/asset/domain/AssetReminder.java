package com.eldercare.backend.roombedallocation.internal.asset.domain;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.util.List;

/** 019 BR-M03-17: nhắc hạn, quá hạn bảo trì, đăng kiểm, bảo hiểm. Sự kiện nội bộ của M03. */
public record AssetReminder(long assetId, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public AssetReminder {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
