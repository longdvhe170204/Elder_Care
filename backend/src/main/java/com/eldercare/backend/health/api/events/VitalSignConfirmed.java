package com.eldercare.backend.health.api.events;

import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import java.time.Instant;
import java.util.Set;

/** Chỉ số vừa xác nhận; giữ phiên bản ngưỡng đã áp tại lúc đo (FR-013, FR-014). */
public record VitalSignConfirmed(
        long recordId,
        long residentId,
        long indicatorId,
        VitalSignLevel level,
        Long thresholdVersionId,
        Long taskId,
        Instant measuredAt,
        Set<VitalSignLabel> labels,
        String eventKey,
        Instant occurredAt) {
    public VitalSignConfirmed {
        labels = Set.copyOf(labels);
    }
}
