package com.eldercare.backend.health.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Lệnh ghi nhận bản ghi chỉ số (FR-010 đến FR-014). */
public record RecordVitalSignCommand(
        long residentId,
        long indicatorId,
        Instant measuredAt,
        long measuredBy,
        Map<String, BigDecimal> values,
        boolean immediateAction,
        Long taskId,
        Instant deviceMeasuredAt,
        boolean offline,
        String note) {
    public RecordVitalSignCommand {
        values = values != null ? Map.copyOf(values) : Map.of();
    }
}
