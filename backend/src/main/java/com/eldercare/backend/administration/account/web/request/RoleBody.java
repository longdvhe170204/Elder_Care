package com.eldercare.backend.administration.account.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RoleBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        StaffRoleCode role,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
