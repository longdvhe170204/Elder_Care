package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Thân Gia hạn dự kiến trở lại (contracts/api-004.yaml, POST {@code /absences/{id}/extend-expected-return}; FR-049). */
public record ExtendExpectedReturnBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expectedReturnAt,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
