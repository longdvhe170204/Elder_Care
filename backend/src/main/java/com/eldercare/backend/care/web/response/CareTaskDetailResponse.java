package com.eldercare.backend.care.web.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(name = "CareTaskDetail")
public record CareTaskDetailResponse(
        @JsonUnwrapped CareTaskResponse task,
        CareResultResponse result,
        String reason,
        String closedBy,
        String closureRule,
        List<Handling> handlingHistory,
        List<String> hiddenFields) {
    public record Handling(String type, String actor, String basis, Instant at, String reason) {}
}
