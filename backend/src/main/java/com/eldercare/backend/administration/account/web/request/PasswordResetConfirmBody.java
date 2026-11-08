package com.eldercare.backend.administration.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Thân {@code POST /auth/password-reset/confirm} (002 T105; FR-052a). */
public record PasswordResetConfirmBody(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        String newPassword) {}
