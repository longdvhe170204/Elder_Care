package com.eldercare.backend.health.internal.schedule.domain;

import com.eldercare.backend.health.api.dto.MeasurementFrequency;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Tần suất lịch đo (FR-003): mốc giờ trong ngày ({@code times}), mỗi N giờ từ một mốc ({@code everyHours},
 * {@code anchorAt}, trống thì lấy thời điểm bắt đầu), hoặc theo thứ trong tuần ISO 1–7 ({@code weekdays}) kèm mốc giờ.
 */
public record ScheduleFrequency(
        MeasurementFrequency type, List<LocalTime> times, Integer everyHours, Instant anchorAt, Set<Integer> weekdays) {
    public ScheduleFrequency {
        times = times == null ? List.of() : List.copyOf(times);
        weekdays = weekdays == null ? Set.of() : Set.copyOf(weekdays);
    }
}
