package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record CareTaskReworkWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long qualityCheckResultId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedAt,
        Long performerId,
        Boolean shared) {}
