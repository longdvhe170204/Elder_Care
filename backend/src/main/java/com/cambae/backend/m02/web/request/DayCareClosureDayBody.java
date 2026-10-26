package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Thân Khai báo ngày khu bán trú nghỉ (contracts/api-004.yaml, POST {@code /day-care-closure-days}; FR-060a). */
public record DayCareClosureDayBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
