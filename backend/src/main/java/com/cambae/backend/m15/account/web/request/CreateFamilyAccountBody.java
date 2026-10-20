package com.cambae.backend.m15.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFamilyAccountBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long familyMemberId,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Verification verification) {}
