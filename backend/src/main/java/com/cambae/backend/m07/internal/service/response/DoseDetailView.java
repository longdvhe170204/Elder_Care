package com.cambae.backend.m07.internal.service.response;

import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.internal.domain.DoseAuthorityBasis;
import com.cambae.backend.m07.internal.domain.DoseCancellationReason;
import com.cambae.backend.m07.internal.domain.DoseResultType;
import com.cambae.backend.m07.internal.domain.DoseStatus;
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
