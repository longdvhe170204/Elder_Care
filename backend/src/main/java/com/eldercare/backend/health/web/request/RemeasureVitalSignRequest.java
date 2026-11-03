package com.eldercare.backend.health.web.request;

import com.eldercare.backend.health.api.dto.RemeasureVitalSignCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Thân lệnh đo lại chỉ số theo hợp đồng 007. */
public record RemeasureVitalSignRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Map<String, BigDecimal> values,
        Instant measuredAt,
        String notes) {

    public RemeasureVitalSignCommand toCommand(long draftId, long actorId) {
        return new RemeasureVitalSignCommand(draftId, measuredAt, actorId, values != null ? values : Map.of(), notes);
    }
}
