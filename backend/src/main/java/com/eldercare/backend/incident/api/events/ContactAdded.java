package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Sự kiện/yêu cầu công khai theo hợp đồng 007 mục 1.12–1.13; không trực tiếp gửi thông báo. */
public record ContactAdded(long residentId, Instant at, String eventKey, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public ContactAdded {
        notificationRequests = List.copyOf(notificationRequests);
    }

    /** Giữ lời gọi nguồn đọc của 003; publisher FR-062 truyền đầy đủ yêu cầu thông báo. */
    public ContactAdded(long residentId, Instant at) {
        this(residentId, at, "M05.CONTACT_ADDED:" + residentId + ":" + at, List.of());
    }
}
