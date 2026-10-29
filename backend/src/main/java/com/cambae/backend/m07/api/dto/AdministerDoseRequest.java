package com.cambae.backend.m07.api.dto;

import java.time.Instant;

/**
 * Request payload to record a dose as administered (DA_DUNG; FR-020, FR-026).
 */
public record AdministerDoseRequest(
        Instant administeredAt,
        Instant deviceRecordedAt,
        String reportedByType,
        Long reportedById,
        String reportedByName,
        String relationship,
        String adverseReaction,
        String note,
        boolean offlineFlag,
        Long leaveHandoverId) {}
