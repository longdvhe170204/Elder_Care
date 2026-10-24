package com.cambae.backend.m15.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Thân {@code POST /resident-accounts} (FR-067). Thiếu {@code consent} thì lệnh trả 422, nên không gắn {@code @NotNull}. */
public record CreateResidentAccountBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long residentId,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UnlockResidentBody.ResidentConsent consent) {}
