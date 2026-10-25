package com.cambae.backend.m02.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Thân Ghi nhận đã ký (contracts/api-004.yaml, {@code POST /contracts/{id}/record-signed}; FR-021). */
public record RecordSignedBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate signedDate,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long signingRepresentativeId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Tệp đã tải lên qua FileApi")
        Long scanFileId) {}
