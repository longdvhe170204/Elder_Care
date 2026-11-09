package com.eldercare.backend.billing.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CostExportRequest(
        @NotNull @Pattern(regexp = "ALL|NEVER_EXPORTED") @Schema(allowableValues = {"ALL", "NEVER_EXPORTED"})
        String scope,

        @NotNull @Pattern(regexp = "XLSX|CSV") @Schema(allowableValues = {"XLSX", "CSV"})
        String format) {}
