package com.eldercare.backend.billing.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(name = "CostAdjustment")
public record CostAdjustmentRequest(
        @NotNull Long amount,

        @NotNull
        @Pattern(regexp = "POST_FINALIZATION_CORRECTION|SOURCE_CHANGE|WAIVER|LATE_ADDITION")
        @Schema(allowableValues = {"POST_FINALIZATION_CORRECTION", "SOURCE_CHANGE", "WAIVER", "LATE_ADDITION"})
        String adjustmentType,

        @NotNull @Valid Reason reason,
        Long sourceChangeId) {}
