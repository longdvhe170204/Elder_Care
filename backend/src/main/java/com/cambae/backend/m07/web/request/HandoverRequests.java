package com.cambae.backend.m07.web.request;

import com.cambae.backend.m07.web.request.DoseRequests.ExternalPersonRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Thân lệnh thuốc mang theo của api-006 (FR-047, FR-048a, FR-048b). */
public final class HandoverRequests {

    private HandoverRequests() {}

    public record HandoverItemRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Long prescriptionId,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Positive
            BigDecimal quantity,

            Long familyMedicationBatchId) {}

    /** api-006 {@code NewMedicationHandover}. */
    public record NewMedicationHandoverRequest(
            Long tripId,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Valid
            ExternalPersonRequest recipient,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotEmpty
            List<@Valid HandoverItemRequest> items) {}

    public record ReturnItemRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Long prescriptionId,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @PositiveOrZero
            BigDecimal quantity) {}

    /** Nhận lại thuốc mang theo, kể cả số lượng 0 kèm lý do (FR-047). */
    public record ReturnHandoverRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            List<@Valid ReturnItemRequest> items,

            @Size(min = 1, max = 2000) String reason) {}

    /** Lệnh "Không mang thuốc" cho một chuyến đi (FR-048b). */
    public record NoMedicationOrderRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Long tripId,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 2000)
            String reason) {}
}
