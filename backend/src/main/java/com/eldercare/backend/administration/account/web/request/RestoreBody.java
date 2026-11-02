package com.eldercare.backend.administration.account.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân lệnh khôi phục một quyền đã thu hẹp (contracts {@code RestoreBody}; FR-025). */
public record RestoreBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long restrictionId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
