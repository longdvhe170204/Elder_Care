package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.medication.internal.domain.DoseResultType;
import java.time.Instant;

/**
 * Payload content for offline dose result synchronization (M07.KET_QUA_LIEU; research D-11).
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
        Long recorderId) {}
