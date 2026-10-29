package com.eldercare.backend.medication.web.response;

import com.eldercare.backend.medication.internal.domain.SuppliedMedicationIntake;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Một lần tiếp nhận thuốc cung ứng (FR-054). */
public record SuppliedMedicationIntakeResponse(
        long id,
        long requestLineId,
        long purchaseRequestId,
        long residentId,
        long prescriptionId,
        String medicationName,
        String concentration,
        String dosageForm,
        BigDecimal receivedQuantity,
        String unit,
        LocalDate expiryDate,
        String condition,
        @Schema(allowableValues = {"PASSED", "NOT_PASSED"}) String result,
        String discrepancy,
        String reason,
        String delivererName,
        @Schema(description = "Họ tên người nhận") String receiverName,
        Instant receivedAt) {

    public static SuppliedMedicationIntakeResponse from(SuppliedMedicationIntake i, String receiverName) {
        return new SuppliedMedicationIntakeResponse(
                i.getId(),
                i.getProcurementLineId(),
                i.getProcurementRequestId(),
                i.getResidentId(),
                i.getPrescriptionId(),
                i.getMedicationName(),
                i.getConcentration(),
                i.getDosageForm(),
                i.getQuantityReceived(),
                i.getUnit(),
                i.getExpiryDate(),
                i.getCondition(),
                i.getIntakeResult().name(),
                i.getDiscrepancy(),
                i.getReason(),
                i.getDelivererName(),
                receiverName,
                i.getReceivedAt());
    }
}
