package com.eldercare.backend.health.web.response;

import com.eldercare.backend.health.api.dto.MeasurementFrequency;
import com.eldercare.backend.health.internal.schedule.domain.MeasurementSchedule;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Lịch đo trả qua HTTP, schema {@code MeasurementSchedule} của api-007. */
public record MeasurementScheduleResponse(
        @ArraySchema(minItems = 1) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> indicatorIds,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ScheduleFrequencyResponse frequency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startAt,
        Instant endAt,

        @Schema(
                allowableValues = {"NORMAL", "IMPORTANT", "MANDATORY"},
                defaultValue = "MANDATORY")
        String priority,

        String notes,
        Long id,

        @Schema(allowableValues = {"PENDING", "EFFECTIVE", "STOPPED", "EXPIRED"})
        String status,

        Long replaces,
        Instant stoppedAt,
        String stopReason) {

    public record ScheduleFrequencyResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            MeasurementFrequency type,

            @ArraySchema(schema = @Schema(pattern = "^[0-2][0-9]:[0-5][0-9]$"))
            List<String> times,

            @Schema(minimum = "1") Integer everyHours,
            Instant anchorAt,

            @ArraySchema(schema = @Schema(minimum = "1", maximum = "7"))
            List<Integer> weekdays) {}

    public static MeasurementScheduleResponse from(MeasurementSchedule schedule) {
        var content = schedule.content();
        var f = content.frequency();
        return new MeasurementScheduleResponse(
                content.indicatorIds().stream().sorted().toList(),
                new ScheduleFrequencyResponse(
                        f.type(),
                        f.times().stream().map(Object::toString).toList(),
                        f.everyHours(),
                        f.anchorAt(),
                        f.weekdays().stream().sorted().toList()),
                content.startAt(),
                content.endAt(),
                content.priority(),
                content.notes(),
                schedule.id(),
                schedule.status(),
                schedule.replaces(),
                schedule.stoppedAt(),
                schedule.stopReason());
    }
}
