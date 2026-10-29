package com.eldercare.backend.medication.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** api-006 {@code NewPrescription} (FR-004 → FR-011). */
public record NewPrescriptionRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        Long medicationId,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Positive
        BigDecimal dosage,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String route,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        PrescriptionType prescriptionType,

        @Valid PrescriptionFrequency frequency,
        Integer minIntervalMinutes,
        Integer maxDosesPer24h,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        Instant startedAt,

        LocalDate endedAt,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        MedicationSource medicationSource,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Valid
        PrescriberInput prescriber,

        String instructionNote,
        List<@Valid IssueConfirmationInput> issueConfirmations) {

    public enum PrescriptionType {
        PERIODIC,
        PRN
    }

    public enum MedicationSource {
        FACILITY,
        FAMILY_SENT
    }

    /** api-006 {@code Frequency}. */
    public record PrescriptionFrequency(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            FrequencyType type,

            List<String> timedSlots,
            Integer everyNHours,
            Instant firstSlot,
            List<Integer> daysOfWeek) {}

    public enum FrequencyType {
        TIMED_SLOTS,
        EVERY_N_HOURS,
        DAYS_OF_WEEK
    }

    /** api-006 {@code Prescriber}; người kê nội bộ là người dùng hiện tại. */
    public record PrescriberInput(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            PrescriberType type,

            String facilityName,
            String prescriberName,
            LocalDate prescribedAt,
            Long prescriptionFileId) {}

    public enum PrescriberType {
        INTERNAL,
        EXTERNAL
    }

    /** Lý do xác nhận một vấn đề trùng hoạt chất, dị ứng (FR-007). */
    public record IssueConfirmationInput(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
            String issueCode,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 2000)
            String reason) {}
}
