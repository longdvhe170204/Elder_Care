package com.eldercare.backend.medication.api.dto;

import java.time.Instant;
import java.util.List;

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
        Long leaveHandoverId,
        List<DoseMeasurement> measurements) {

    /** Lệnh không kèm chỉ số đo trước khi dùng. */
    public AdministerDoseRequest(
            Instant administeredAt,
            Instant deviceRecordedAt,
            String reportedByType,
            Long reportedById,
            String reportedByName,
            String relationship,
            String adverseReaction,
            String note,
            boolean offlineFlag,
            Long leaveHandoverId) {
        this(
                administeredAt,
                deviceRecordedAt,
                reportedByType,
                reportedById,
                reportedByName,
                relationship,
                adverseReaction,
                note,
                offlineFlag,
                leaveHandoverId,
                null);
    }
}
