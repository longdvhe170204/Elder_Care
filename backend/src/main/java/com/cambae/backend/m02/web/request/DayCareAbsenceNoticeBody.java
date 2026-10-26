package com.cambae.backend.m02.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

/** Thân Ghi nhận gia đình báo vắng bán trú (contracts/api-004.yaml, POST {@code /day-care-absence-notices}; FR-060). */
public record DayCareAbsenceNoticeBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long residentId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,

        @Schema(description = "Bỏ trống là cả ngày") Long sessionId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant familyNotifiedAt) {}
