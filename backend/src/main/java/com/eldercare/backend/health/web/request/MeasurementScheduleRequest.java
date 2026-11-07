package com.eldercare.backend.health.web.request;

import com.eldercare.backend.health.api.dto.MeasurementFrequency;
import com.eldercare.backend.health.internal.schedule.domain.NewMeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.ScheduleFrequency;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/** Thân lệnh tạo lịch đo, schema {@code NewMeasurementSchedule} của api-007 (FR-003). */
public record MeasurementScheduleRequest(
        @ArraySchema(minItems = 1) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> indicatorIds,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FrequencyRequest frequency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startAt,
        Instant endAt,

        @Schema(
                allowableValues = {"NORMAL", "IMPORTANT", "MANDATORY"},
                defaultValue = "MANDATORY")
        String priority,

        String notes) {

    /** Tần suất: mốc giờ {@code HH:mm}, mỗi N giờ từ một mốc, hoặc theo thứ ISO 1–7 kèm mốc giờ. */
    public record FrequencyRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            MeasurementFrequency type,

            @ArraySchema(schema = @Schema(pattern = "^[0-2][0-9]:[0-5][0-9]$"))
            List<String> times,

            @Schema(minimum = "1") Integer everyHours,
            Instant anchorAt,

            @ArraySchema(schema = @Schema(minimum = "1", maximum = "7"))
            List<Integer> weekdays) {}

    public NewMeasurementSchedule toDomain() {
        var f = frequency;
        var frequencyDomain = f == null
                ? null
                : new ScheduleFrequency(
                        f.type(),
                        f.times() == null
                                ? List.of()
                                : f.times().stream().map(LocalTime::parse).toList(),
                        f.everyHours(),
                        f.anchorAt(),
                        f.weekdays() == null ? Set.of() : Set.copyOf(f.weekdays()));
        return new NewMeasurementSchedule(
                indicatorIds == null ? Set.of() : Set.copyOf(indicatorIds),
                frequencyDomain,
                startAt,
                endAt,
                priority,
                notes);
    }
}
