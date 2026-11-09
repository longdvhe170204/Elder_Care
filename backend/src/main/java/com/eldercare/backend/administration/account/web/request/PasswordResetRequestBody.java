package com.eldercare.backend.administration.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Thân {@code POST /auth/password-reset/request} (002 T105; FR-052a). */
public record PasswordResetRequestBody(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String username) {}
