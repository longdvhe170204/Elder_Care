package com.eldercare.backend.administration.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateStaffAccountBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long staffId,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotNull @Size(min = 1) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<StaffRoleCode> roles) {}
