package com.eldercare.backend.staffing.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Sự kiện 008 mục 1.6; dịch vụ nguồn phát trong transaction, Modulith chuyển sau commit. */
public record ShiftLeadMissing(long shiftId, Instant occurredAt, List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public ShiftLeadMissing {
        notificationRequests = List.copyOf(notificationRequests);
    }

    public String eventKey() {
        return "SHIFT_LEAD_MISSING:" + shiftId + ":" + occurredAt;
    }
}
