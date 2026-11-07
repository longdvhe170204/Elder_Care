package com.eldercare.backend.health.internal.schedule.domain;

import com.eldercare.backend.health.api.dto.MeasurementFrequency;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Dạng JSON của nội dung lịch đo trong cột {@code components} (R-15: {@code lich_do_chi_so}, tần suất → JSON). */
public record StoredScheduleContent(
        List<Long> indicatorIds,
        MeasurementFrequency frequencyType,
        List<String> times,
        Integer everyHours,
        Instant anchorAt,
        Set<Integer> weekdays,
        Instant endAt,
        String priority,
        String notes) {}
