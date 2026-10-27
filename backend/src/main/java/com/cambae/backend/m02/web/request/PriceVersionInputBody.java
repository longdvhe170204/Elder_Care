package com.cambae.backend.m02.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Thân tạo phiên bản đơn giá (contracts/api-004.yaml, POST {@code /services/{id}/price-versions}; FR-035). */
public record PriceVersionInputBody(
        @NotNull @Min(0) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long unitPrice,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate effectiveFrom,

        LocalDate effectiveTo) {}
