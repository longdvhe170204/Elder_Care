package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân chỉ có lý do (contracts/api-004.yaml, {@code requestBodies/ReasonBody}). */
@Schema(name = "ReasonBody")
public record ReasonBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
