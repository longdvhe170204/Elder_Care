package com.cambae.backend.m15.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân {@code POST /auth/qr-login} (FR-069). */
public record QrLoginBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String code) {}
