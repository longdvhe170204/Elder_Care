package com.cambae.backend.m07.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/** Thân các lệnh đổi đơn của api-006 (FR-011, FR-014 → FR-017). */
public final class PrescriptionChangeRequests {

    private PrescriptionChangeRequests() {}

    /** Ngừng đơn, ngay hoặc từ {@code discontinueAt} (FR-015, BR-M07-15). */
    public record DiscontinueRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 2000)
            String reason,

            @Valid ExternalBasisRequest externalBasis,
            Instant discontinueAt) {}

    /** Gia hạn; {@code newEndedAt} rỗng là bỏ ngày kết thúc (FR-017). */
    public record ExtendRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 2000)
            String reason,

            @Valid ExternalBasisRequest externalBasis,
            LocalDate newEndedAt) {}

    /** Đổi liều, đổi thuốc: ngừng đơn cũ và tạo đơn thay thế (FR-016, DBR-14). */
    public record ChangeDosageRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Valid
            NewPrescriptionRequest newPrescription,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Instant effectiveFrom,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 2000)
            String reason,

            @Valid ExternalBasisRequest externalBasis) {}

    /** Ghi chú hướng dẫn, trường duy nhất sửa được của đơn (FR-014). */
    public record InstructionNoteRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Size(max = 2000)
            String instructionNote) {}
}
