package com.eldercare.backend.medication.api.events;

import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/**
 * Event requesting a medical alert regarding medication.
 * M05 (007) listens and creates or merges the alert; notifications follow the created alert. Not a
 * {@code HasNotificationRequests}: only M13 may listen to those (M13ArchTest), and every producer sends an empty list.
 */
public record MedicationAlertRequestedEvent(
        String alertType, // MISSED_DOSE | ADVERSE_REACTION | CONSECUTIVE_REFUSALS | ALLERGY_CONFLICT | ...
        String alertLevel, // LOW | MEDIUM | CRITICAL
        long residentId,
        String sourceRecord,
        String deduplicationKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests) {}
