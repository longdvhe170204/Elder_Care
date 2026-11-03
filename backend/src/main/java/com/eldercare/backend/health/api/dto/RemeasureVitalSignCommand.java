package com.eldercare.backend.health.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Lệnh đo lại bản ghi chỉ số đang chờ đo lại (FR-016). */
public record RemeasureVitalSignCommand(
        long originalRecordId, Instant measuredAt, long measuredBy, Map<String, BigDecimal> values, String note) {
    public RemeasureVitalSignCommand {
        values = values != null ? Map.copyOf(values) : Map.of();
    }
}
