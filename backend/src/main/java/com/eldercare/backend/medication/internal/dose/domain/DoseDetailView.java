package com.eldercare.backend.medication.internal.dose.domain;

import com.eldercare.backend.medication.api.dto.MedicationSource;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Complete view of a medication dose and its outcome (FR-018 -> FR-023).
 */
public record DoseDetailView(
        String doseKey,
        Long doseId,
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
        Instant administeredAt,
        DoseResultType resultType,
        DoseAuthorityBasis authorityBasis,
        Long recorderId,
        String reason,
        String adverseReaction,
        String note,
        boolean lateFlag,
        boolean offlineFlag,
        Long assignedNurseId,
        boolean floorShared) {}
