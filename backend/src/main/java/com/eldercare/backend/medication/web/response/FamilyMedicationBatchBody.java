package com.eldercare.backend.medication.web.response;

import com.eldercare.backend.medication.internal.service.response.FamilyMedicationBatchResponse;
import com.eldercare.backend.medication.web.response.DoseBodies.PersonBody;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** api-006 {@code FamilyMedicationBatch} (FR-040, FR-045; không có số lượng còn, Q-393). */
public record FamilyMedicationBatchBody(
        Long medicationId,
        String customName,
        String customActiveIngredient,
        @Schema(requiredMode = RequiredMode.REQUIRED) String concentration,
        @Schema(requiredMode = RequiredMode.REQUIRED) BigDecimal quantity,
        @Schema(requiredMode = RequiredMode.REQUIRED) String unit,
        @Schema(requiredMode = RequiredMode.REQUIRED) LocalDate expiryDate,
        @Schema(requiredMode = RequiredMode.REQUIRED) String packagingCondition,
        @Schema(requiredMode = RequiredMode.REQUIRED) Boolean hasPrescription,
        List<Long> fileIds,
        @Schema(requiredMode = RequiredMode.REQUIRED) PersonBody deliverer,
        Long purchaseAssistanceId,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant receivedAt,
        Long id,

        @Schema(allowableValues = {"PENDING_RECONCILIATION", "IN_USE", "HOLD_ONLY", "DEPLETED", "RETURNED", "DISPOSED"})
        String status,

        List<Long> boundPrescriptionIds,
        BigDecimal finalQuantity,
        Instant closedAt) {

    public static FamilyMedicationBatchBody of(FamilyMedicationBatchResponse b) {
        return new FamilyMedicationBatchBody(
                b.getMedicationId(),
                b.getCustomName(),
                b.getCustomActiveIngredient(),
                b.getCustomConcentration(),
                b.getInitialQuantity(),
                b.getUnit(),
                b.getExpiryDate(),
                b.getPackagingCondition(),
                b.isHasPrescription(),
                b.getFileIds(),
                new PersonBody(
                        personType(b.getDelivererType()),
                        b.getDelivererId(),
                        b.getDelivererName(),
                        b.getRelationship()),
                b.getPurchaseRequestId(),
                b.getReceivedAt(),
                b.getId(),
                b.getStatus(),
                b.getBoundPrescriptionIds(),
                b.getFinalQuantity(),
                b.getClosedAt());
    }

    /** Người giao nhân viên mua hộ là {@code STAFF} của {@code ExternalPerson} (Q-139). */
    private static String personType(String delivererType) {
        if (delivererType == null) {
            return null;
        }
        return "PURCHASING_STAFF".equals(delivererType) ? "STAFF" : delivererType;
    }
}
