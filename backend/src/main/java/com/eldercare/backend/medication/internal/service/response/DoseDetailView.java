package com.eldercare.backend.medication.internal.service.response;

import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.domain.DoseCancellationReason;
import com.eldercare.backend.medication.internal.domain.DoseResultType;
import com.eldercare.backend.medication.internal.domain.DoseStatus;
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
