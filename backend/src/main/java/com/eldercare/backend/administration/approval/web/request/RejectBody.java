package com.eldercare.backend.administration.approval.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

public record RejectBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason comment) {}
