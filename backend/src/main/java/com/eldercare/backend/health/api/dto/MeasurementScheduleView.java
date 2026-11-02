package com.eldercare.backend.health.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/** Bản đọc lịch đo hiệu lực; kiểu tần suất và tham số theo FR-003. */
public record MeasurementScheduleView(
        long id,
        long residentId,
        Set<Long> indicatorIds,
        MeasurementFrequency frequencyType,
        Map<String, Object> frequencyParameters,
        Instant startsAt,
        Instant endsAt,
        String importance,
        String note) {
    public MeasurementScheduleView {
        indicatorIds = Set.copyOf(indicatorIds);
        frequencyParameters = Map.copyOf(frequencyParameters);
    }
}
