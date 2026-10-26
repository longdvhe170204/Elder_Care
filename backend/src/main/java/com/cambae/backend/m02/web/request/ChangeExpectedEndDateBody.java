package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Thân Đổi ngày kết thúc dự kiến (contracts/api-004.yaml, POST {@code /residency-endings/{id}/change-date}; FR-067). */
public record ChangeExpectedEndDateBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate expectedEndDate,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
