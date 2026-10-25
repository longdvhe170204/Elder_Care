package com.cambae.backend.m07.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

/** api-006 {@code ExternalBasis} (BR-M07-16): bắt buộc cho lệnh của Điều dưỡng trên đơn bên ngoài. */
public record ExternalBasisRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String facilityName,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String prescriberName,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        LocalDate prescribedDate,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        BasisType basisType,

        String callerName,
        String receiverName,
        Instant calledAt,
        Long documentFileId) {

    public enum BasisType {
        NEW_PRESCRIPTION,
        DISCHARGE_PAPER,
        PHONE_ORDER
    }
}
