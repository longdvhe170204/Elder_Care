package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Các tác động vẫn còn hiệu lực sau khi đính chính hoặc hủy sự cố (FR-046a). */
public record EffectiveImpactsRemain(
        long incidentId,
        List<String> impacts,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public EffectiveImpactsRemain {
        impacts = List.copyOf(impacts);
        notificationRequests = List.copyOf(notificationRequests);
    }
}
