package com.cambae.backend.m07.web.request;

import com.cambae.backend.m07.web.request.DoseRequests.ExternalPersonRequest;
import com.cambae.backend.m07.web.request.DoseRequests.ResultBasis;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** api-006 {@code NewPrnAdministration} (FR-024, FR-025, FR-027, BR-M07-04). */
public record NewPrnAdministrationRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        Instant administeredAt,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Positive
        BigDecimal dosage,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 2000)
        String reason,

        String adverseReaction,
        String note,
        Long familyMedicationBatchId,
        ResultBasis basis,
        @Valid ExternalPersonRequest reportedBy,
        Boolean recordedAfterDisconnect) {}
