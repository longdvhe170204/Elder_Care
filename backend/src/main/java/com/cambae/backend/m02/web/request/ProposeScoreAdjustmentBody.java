package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Thân Lập đề nghị điều chỉnh điểm (contracts/api-004.yaml, POST {@code /waiting-list-entries/{id}/propose-score-adjustment}):
 * dương là cộng, âm là trừ, khác 0, kèm lý do (FR-011).
 */
public record ProposeScoreAdjustmentBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Dương là cộng, âm là trừ; khác 0")
        Integer points,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
