package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.internal.domain.DoseCancellationReason;
import com.cambae.backend.m07.internal.domain.DoseStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projected dose slot computed dynamically by pure schedule calculator (research D-06, FR-018).
 * doseKey format: {prescriptionId}-{yyyyMMddHHmm} (Asia/Ho_Chi_Minh).
 */
public record CalculatedDoseSlot(
        String doseKey,
        Long prescriptionId,
        Long residentId,
        Long medicationId,
        String medicationName,
        BigDecimal dosage,
        String dosageUnit,
        String route,
        MedicationSource medicationSource,
        boolean specialControl,
        Instant scheduledTime,
        Instant windowFrom,
        Instant windowTo,
        DoseStatus status,
        DoseCancellationReason cancellationReason,
        boolean materialized,
        Long materializedDoseId) {}
