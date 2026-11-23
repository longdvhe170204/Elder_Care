package com.eldercare.backend.medication.web.request;

import com.eldercare.backend.medication.web.request.DoseRequests.ExternalPersonRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Thân lệnh thuốc gia đình gửi của api-006 (FR-040 → FR-046). */
public final class FamilyMedicationRequests {

    private FamilyMedicationRequests() {}

    /** api-006 {@code ReceiveFamilyMedication}: tiếp nhận một lô. */
    public record ReceiveFamilyMedicationRequest(
            Long medicationId,
            String customName,
            String customActiveIngredient,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
            String concentration,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            BigDecimal quantity,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
            String unit,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            LocalDate expiryDate,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
            String packagingCondition,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Boolean hasPrescription,

            List<Long> fileIds,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Valid
            ExternalPersonRequest deliverer,

            Long purchaseAssistanceId,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Instant receivedAt,

            @Valid IntakeChecks intakeChecks) {}

    /** Kết quả kiểm tra khi tiếp nhận: mỗi mục Đạt (true) hoặc Không đạt (false); BR-M07-30. */
    public record IntakeChecks(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Boolean labelOk,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Boolean expiryOk,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Boolean packagingOk,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Boolean nameOk) {}

    public enum UseBasis {
        FAMILY_PRESCRIPTION,
        FACILITY_PRESCRIPTION
    }

    /** Xác nhận sử dụng, gắn đơn nguồn gia đình gửi (FR-041, FR-043, BR-M07-10). */
    public record ConfirmUseRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotEmpty
            List<Long> prescriptionIds,

            Long medicationId,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Boolean packagingRecognizable,

            UseBasis basis) {}

    /** Báo gia đình sắp hết, ghi chú tùy chọn (FR-046, Q-393). */
    public record NotifyRunningLowRequest(@Size(max = 2000) String note) {}

    /** Hoàn trả cả lô, số lượng đếm thực tế (BR-M07-19, Q-393). */
    public record ReturnFamilyMedicationRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            BigDecimal quantity,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Valid
            ExternalPersonRequest recipient,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Instant returnedAt,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
            String witness) {}

    public enum DisposeKind {
        FAMILY_REQUEST,
        EXPIRED
    }

    /**
     * Hủy cả lô, có người chứng kiến (BR-M07-19, BR-M07-30): theo yêu cầu gia đình (mặc định; cần người đại diện, hình thức) hoặc
     * do hết hạn dùng.
     */
    public record DisposeRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            BigDecimal quantity,

            DisposeKind kind,
            Long representativeId,
            String requestMethod,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
            String witness,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 2000)
            String reason) {}
}
