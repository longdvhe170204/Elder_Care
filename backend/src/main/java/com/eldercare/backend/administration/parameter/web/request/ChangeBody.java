package com.eldercare.backend.administration.parameter.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

public record ChangeBody(
        @Schema(implementation = Object.class) JsonNode value,
        Boolean restoreDefault,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
