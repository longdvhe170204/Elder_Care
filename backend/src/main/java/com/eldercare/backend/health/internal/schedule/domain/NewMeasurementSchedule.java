package com.eldercare.backend.health.internal.schedule.domain;

import java.time.Instant;
import java.util.Set;

/** Nội dung lịch đo của FR-003; mức quan trọng {@code NORMAL}, {@code IMPORTANT}, {@code MANDATORY} (mặc định). */
public record NewMeasurementSchedule(
        Set<Long> indicatorIds,
        ScheduleFrequency frequency,
        Instant startAt,
        Instant endAt,
        String priority,
        String notes) {
    public NewMeasurementSchedule {
        indicatorIds = indicatorIds == null ? Set.of() : Set.copyOf(indicatorIds);
        priority = priority == null ? "MANDATORY" : priority;
    }
}
