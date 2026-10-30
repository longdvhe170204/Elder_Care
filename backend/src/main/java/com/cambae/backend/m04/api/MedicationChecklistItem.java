package com.cambae.backend.m04.api;

import java.time.Instant;

/**
 * Checklist item view for medication administration in shifts (005 FR-029, FR-048).
 */
public record MedicationChecklistItem(
        String doseKey,
        long residentId,
        long prescriptionId,
        Instant scheduledTime,
        Instant windowFrom,
        Instant windowTo,
        String status,
        boolean floorShared,
        String source,
        String medicationName,
        String dosage) {}
