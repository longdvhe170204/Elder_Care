package com.cambae.backend.m15.account.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReasonBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
