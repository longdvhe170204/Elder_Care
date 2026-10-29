package com.eldercare.backend.staffing.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Sự kiện 008 mục 1.6; dịch vụ nguồn phát trong transaction, Modulith chuyển sau commit. */
public record CredentialExpiring(
        long credentialId,
        long staffId,
        String credentialType,
        LocalDate expiryDate,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public CredentialExpiring {
        notificationRequests = List.copyOf(notificationRequests);
    }

    public String eventKey() {
        return "CREDENTIAL_EXPIRING:" + credentialId + ":" + occurredAt;
    }
}
