package com.eldercare.backend.medication.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** Thân lệnh đối chiếu thuốc của api-006 (FR-034 → FR-039). */
public final class ReconciliationRequests {

    private ReconciliationRequests() {}

    public enum ReconciliationReason {
        ADMISSION,
        HOSPITAL_RETURN,
        PROFESSIONAL_REQUEST
    }

    public enum ItemSource {
        CURRENT_PRESCRIPTION,
        DISCHARGE_PRESCRIPTION,
        ADMISSION_CURRENT_MEDICATION
    }

    public enum Decision {
        CONTINUE,
        DISCONTINUE,
        CHANGE_DOSE,
        ADD_NEW
    }

    /** Lập phiếu; {@code reasonDetail} bắt buộc khi theo yêu cầu chuyên môn (FR-034b). */
    public record NewReconciliationRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            ReconciliationReason reason,

            String reasonDetail) {}

    /** api-006 {@code ReconciliationItem} khi thay danh sách dòng của phiếu Mở. */
    public record ReconciliationItemRequest(
            Long id,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            ItemSource source,

            Long prescriptionId,
            Long admissionMedicationItemId,
            String medicationDescription,
            Decision decision,
            String reason,
            @Valid NewPrescriptionRequest newPrescription) {}

    /** Đề nghị đính chính phiếu Đã xác nhận (FR-039, BR-M07-17). */
    public record ProposeCorrectionRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Map<String, Object> correctedContent,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 2000)
            String reason) {}
}
