package com.eldercare.backend.medication.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** api-006 {@code NewSuppliedMedicationIntake} (FR-054). */
public record NewSuppliedMedicationIntakeBody(
        @NotNull Long requestLineId,
        @NotBlank String concentration,
        @NotBlank String dosageForm,
        @NotNull @Positive BigDecimal receivedQuantity,
        @NotBlank String unit,
        @NotNull LocalDate expiryDate,
        @NotBlank String condition,
        @NotNull IntakeResultValue result,
        String discrepancy,
        String reason,
        @NotBlank String delivererName) {

    public enum IntakeResultValue {
        PASSED,
        NOT_PASSED
    }
}
