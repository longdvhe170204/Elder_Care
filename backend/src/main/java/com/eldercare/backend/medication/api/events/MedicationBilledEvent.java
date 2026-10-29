package com.eldercare.backend.medication.api.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event fired when medication fee is incurred for facility-provided medication (FR-025a, FR-033, FR-048a).
 * Module M11 (010) listens to record facility charges.
 */
public record MedicationBilledEvent(
        BilledSource source,
        long sourceId,
        long residentId,
        long medicationId,
        BigDecimal quantity,
        Instant occurredAt) {}
