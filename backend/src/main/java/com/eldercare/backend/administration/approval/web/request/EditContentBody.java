package com.eldercare.backend.administration.approval.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import tools.jackson.databind.JsonNode;

public record EditContentBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Object.class)
        JsonNode content,

        LocalDate requestedEffectiveDate,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
