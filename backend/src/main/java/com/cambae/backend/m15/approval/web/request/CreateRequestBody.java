package com.cambae.backend.m15.approval.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import tools.jackson.databind.JsonNode;

public record CreateRequestBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String type,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Object.class)
        JsonNode content,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate requestedEffectiveDate,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason,

        Boolean submitImmediately) {}
