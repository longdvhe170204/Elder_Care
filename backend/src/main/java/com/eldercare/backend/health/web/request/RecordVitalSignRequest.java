package com.eldercare.backend.health.web.request;

import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Thân lệnh ghi chỉ số ngoài lịch theo hợp đồng 007; công việc đo của 005 ghi qua {@code VitalSignPort}. */
public record RecordVitalSignRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long indicatorId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Map<String, BigDecimal> values,
        Instant measuredAt,
        Boolean immediateAction,
        String notes) {

    public RecordVitalSignCommand toCommand(long residentId, long actorId) {
        return new RecordVitalSignCommand(
                residentId,
                indicatorId != null ? indicatorId : 0L,
                measuredAt,
                actorId,
                values != null ? values : Map.of(),
                Boolean.TRUE.equals(immediateAction),
                null,
                null,
                false,
                notes);
    }
}
