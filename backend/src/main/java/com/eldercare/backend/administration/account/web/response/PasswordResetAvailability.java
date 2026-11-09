package com.eldercare.backend.administration.account.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Phản hồi {@code GET /auth/password-reset/availability} (002 T105; FR-052a, Q-06). */
public record PasswordResetAvailability(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean available) {}
