package com.cambae.backend.m15.approval.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

public record RejectBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason comment) {}
