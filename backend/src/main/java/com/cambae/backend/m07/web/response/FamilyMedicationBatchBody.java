package com.cambae.backend.m07.web.response;

import com.cambae.backend.m07.internal.service.response.FamilyMedicationBatchResponse;
import com.cambae.backend.m07.web.response.DoseBodies.PersonBody;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** api-006 {@code FamilyMedicationBatch} (FR-040, FR-045, FR-046). */
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

        BigDecimal remainingQuantity,
        List<Long> boundPrescriptionIds,
        BigDecimal daysRemaining,
        List<MovementBody> movements) {

    public record MovementBody(String movementType, BigDecimal quantity, Instant movedAt, String source) {}

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
                b.getRemainingQuantity(),
                b.getBoundPrescriptionIds(),
                b.getDaysRemaining() == null ? null : BigDecimal.valueOf(b.getDaysRemaining()),
                b.getMovements() == null
                        ? List.of()
                        : b.getMovements().stream()
                                .map(m -> new MovementBody(
                                        m.getMovementType(), m.getQuantity(), m.getMovedAt(), m.getReason()))
                                .toList());
    }

    /** Người giao nhân viên mua hộ là {@code STAFF} của {@code ExternalPerson} (Q-139). */
    private static String personType(String delivererType) {
        if (delivererType == null) {
            return null;
        }
        return "PURCHASING_STAFF".equals(delivererType) ? "STAFF" : delivererType;
    }
}
