package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Thân lệnh ghi diễn biến sự cố (api-007 {@code POST /incidents/{id}/progressions}). */
public record AddProgressionRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long progressionTypeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        Instant occurredAt) {}
