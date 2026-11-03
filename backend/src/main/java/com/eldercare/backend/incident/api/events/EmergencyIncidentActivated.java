package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/** Quy trình sự cố khẩn cấp đã kích hoạt; yêu cầu người thân phải mang mã Q-354 (FR-047, FR-047c). */
public record EmergencyIncidentActivated(
        long incidentId,
        long residentId,
        boolean noOnDutyDoctor,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {

    public EmergencyIncidentActivated {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
