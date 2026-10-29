package com.eldercare.backend.administration.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangePasswordBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        String currentPassword,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        String newPassword) {}
