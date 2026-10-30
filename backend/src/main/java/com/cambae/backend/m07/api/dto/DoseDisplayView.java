package com.cambae.backend.m07.api.dto;

import java.time.Instant;
import java.util.Optional;

/**
 * Medication dose display view for schedule and checklist.
 */
public record DoseDisplayView(
        String doseKey,
        long residentId,
        long prescriptionId,
        Instant scheduledTime,
        Instant windowStart,
        Instant windowEnd,
        DoseStatus status,
        boolean floorSharedDose,
        boolean pendingHandoverConfirmation,
        boolean awaitingRecordAfterReturn,
        boolean requiresOnline,
        MedicationSource source,
        Optional<Long> dispenseRecordId,
        Optional<MedicationSummary> medication) {}
