package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.medication.internal.dose.domain.DoseResultType;
import java.time.Instant;

/**
 * Payload content for offline dose result synchronization (M07.DOSE_RESULT; research D-11).
 */
public record DoseResultOfflineContent(
        String doseKey,
        Long residentId,
        DoseResultType resultType,
        Instant administeredAt,
        String reason,
        String adverseReaction,
        String note,
        Long leaveHandoverId,
        java.util.List<com.eldercare.backend.medication.api.dto.DoseMeasurement> measurements) {

    public DoseResultOfflineContent(
            String doseKey,
            Long residentId,
            DoseResultType resultType,
            Instant administeredAt,
            String reason,
            String adverseReaction,
            String note,
            Long leaveHandoverId) {
        this(doseKey, residentId, resultType, administeredAt, reason, adverseReaction, note, leaveHandoverId, null);
    }
}
