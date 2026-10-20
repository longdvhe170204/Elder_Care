package com.cambae.backend.m15.account.web.request;

import com.cambae.backend.m15.account.api.dto.AccessChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginBody(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        String password,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        AccessChannel channel) {}
