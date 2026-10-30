package com.cambae.backend.m07.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Thân chỉ có lý do (api-006 request body {@code Reason}). */
public record ReasonRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 2000)
        String reason) {}
