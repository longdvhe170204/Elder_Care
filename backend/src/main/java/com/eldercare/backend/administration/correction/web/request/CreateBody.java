package com.eldercare.backend.administration.correction.web.request;

import com.eldercare.backend.administration.correction.api.CorrectionType;
import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CorrectionType type,

        Object correctedContent,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
