package com.eldercare.backend.health.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.health.web.request.MeasurementScheduleRequest.FrequencyRequest;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Thân lệnh Thay đổi lịch đo: nội dung lịch mới cùng lý do bắt buộc (FR-005). */
public record ChangeMeasurementScheduleRequest(
        @ArraySchema(minItems = 1) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> indicatorIds,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FrequencyRequest frequency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startAt,
        Instant endAt,

        @Schema(
                allowableValues = {"NORMAL", "IMPORTANT", "MANDATORY"},
                defaultValue = "MANDATORY")
        String priority,

        String notes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {

    public MeasurementScheduleRequest schedule() {
        return new MeasurementScheduleRequest(indicatorIds, frequency, startAt, endAt, priority, notes);
    }
}
