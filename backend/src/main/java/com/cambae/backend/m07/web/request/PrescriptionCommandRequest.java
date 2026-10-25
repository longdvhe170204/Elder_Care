package com.cambae.backend.m07.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** api-006 {@code PrescriptionCommandBody}: Tạm dừng, Tiếp tục đơn (FR-011, FR-013). */
public record PrescriptionCommandRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 2000)
        String reason,

        @Valid ExternalBasisRequest externalBasis) {}
