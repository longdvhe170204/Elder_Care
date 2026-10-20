package com.cambae.backend.m15.correction.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m15.correction.api.CorrectionType;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CorrectionType type,

        Object correctedContent,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
