package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m02.internal.absence.domain.BedHoldChoice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Thân Quyết định giữ giường (contracts/api-004.yaml, POST {@code /absences/{id}/bed-hold-decision}; FR-055, FR-061).
 * {@code coefficient} bắt buộc theo hợp đồng; riêng Giải phóng có thể bỏ trống để dùng mặc định 0%.
 */
public record BedHoldDecisionBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        BedHoldChoice choice,

        @NotNull @DecimalMin("0") @DecimalMax("100") @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal coefficient,

        LocalDate reviewDate,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
