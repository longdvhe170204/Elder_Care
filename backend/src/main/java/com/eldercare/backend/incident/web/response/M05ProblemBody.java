package com.eldercare.backend.incident.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/** Mô tả lỗi của 007 phần M05 theo schema {@code Problem} của api-007; lỗi thật vẫn do ProblemDetailsHandler xử lý. */
@Schema(name = "M05Problem")
public record M05ProblemBody(
        String type,
        String title,
        Integer status,
        String detail,
        String errorCode,
        List<Condition> unmetConditions,

        @Schema(description = "OUT_OF_PHYSICAL_RANGE: { componentCode: { min, max } }")
        Map<String, Object> validRange,

        @Schema(description = "OPEN_EMERGENCY_INCIDENT_EXISTS")
        Long openIncidentId,

        @Schema(description = "WISH_NOT_CHECKED") Map<String, Object> endOfLifeWish) {

    public record Condition(String code, String description, String basis) {}
}
