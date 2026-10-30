package com.cambae.backend.m07.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/**
 * Event requesting a medical alert regarding medication.
 * Modules M05 (007) and M13 (009) listen to process alerts and send notifications.
 */
public record MedicationAlertRequestedEvent(
        String alertType, // MISSED_DOSE | ADVERSE_REACTION | CONSECUTIVE_REFUSALS | ALLERGY_CONFLICT | ...
        String alertLevel, // LOW | MEDIUM | CRITICAL
        long residentId,
        String sourceRecord,
        String deduplicationKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {}
