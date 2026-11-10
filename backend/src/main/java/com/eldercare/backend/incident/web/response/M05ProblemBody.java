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

    /** 409 {@code ALREADY_RECEIVED} của lệnh Tiếp nhận (api-007): {@code Problem} cộng người, lúc đã tiếp nhận. */
    @Schema(name = "M05ReceivedProblem")
    public record Received(
            String type,
            String title,
            Integer status,
            String detail,
            String errorCode,
            List<Condition> unmetConditions,
            Map<String, Object> validRange,
            Long openIncidentId,
            Map<String, Object> endOfLifeWish,
            String receivedBy,
            java.time.Instant receivedAt) {}
}
