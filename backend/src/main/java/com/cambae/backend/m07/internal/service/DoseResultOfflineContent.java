package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m07.internal.domain.DoseResultType;
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
