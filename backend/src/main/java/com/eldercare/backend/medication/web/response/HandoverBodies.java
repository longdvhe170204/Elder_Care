package com.eldercare.backend.medication.web.response;

import com.eldercare.backend.medication.internal.service.DoseViewService.UsableBatch;
import com.eldercare.backend.medication.internal.service.response.HandoverResponse;
import com.eldercare.backend.medication.internal.service.response.MedicationToBringResponse;
import com.eldercare.backend.medication.web.response.DoseBodies.PersonBody;
import com.eldercare.backend.medication.web.response.DoseBodies.UsableBatchBody;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Phản hồi thuốc mang theo theo api-006 ({@code MedicationHandover}; FR-048a, FR-048b). */
public final class HandoverBodies {

    private HandoverBodies() {}

    public record HandoverItemBody(
            @Schema(requiredMode = RequiredMode.REQUIRED) Long prescriptionId,
            @Schema(requiredMode = RequiredMode.REQUIRED) BigDecimal quantity,
            Long familyMedicationBatchId) {}

    public record HandoverDoseBody(String doseKey, Boolean reverted) {}

    /** api-006 {@code MedicationHandover}. */
    public record MedicationHandoverBody(
            Long tripId,
            @Schema(requiredMode = RequiredMode.REQUIRED) PersonBody recipient,
            @Schema(requiredMode = RequiredMode.REQUIRED) List<HandoverItemBody> items,
            Long id,

            @Schema(allowableValues = {"HANDED_OVER", "WAITING_RETURN", "RETURNED"})
            String status,

            Instant handoverTime,
            List<HandoverDoseBody> doses) {

        public static MedicationHandoverBody of(HandoverResponse h) {
            return new MedicationHandoverBody(
                    h.getTripId(),
                    new PersonBody(
                            personType(h.getRecipientType()),
                            h.getRecipientId(),
                            h.getRecipientName(),
                            h.getRelationship()),
                    h.getItems() == null
                            ? List.of()
                            : h.getItems().stream()
                                    .map(i -> new HandoverItemBody(
                                            i.getPrescriptionId(), i.getQuantity(), i.getFamilyMedicationBatchId()))
                                    .toList(),
                    h.getId(),
                    h.getStatus(),
                    h.getHandoverTime(),
                    h.getDoses().stream()
                            .map(d -> new HandoverDoseBody(d.doseKey(), d.reverted()))
                            .toList());
        }
    }

    /** Người nhận của lần giao theo {@code ExternalPerson}: nhân viên, trưởng đoàn là {@code STAFF}. */
    private static String personType(String recipientType) {
        if (recipientType == null) {
            return null;
        }
        return "RELATIVE".equals(recipientType) ? "FAMILY_MEMBER" : "STAFF";
    }

    /** Thuốc cần mang (bổ sung schema khi dịch 2026-10-26; FR-048a, FR-048b). */
    public record MedicationToBringBody(
            Long prescriptionId,
            String medicationName,
            BigDecimal dosage,
            String dosageUnit,
            @Schema(allowableValues = {"PERIODIC", "PRN"}) String prescriptionType,

            @Schema(allowableValues = {"FACILITY", "FAMILY_SENT"})
            String medicationSource,

            List<UsableBatchBody> usableBatches,

            @Schema(description = "Rỗng khi chưa biết giờ về hoặc đơn PRN")
            BigDecimal totalQuantityNeeded,

            List<Instant> scheduledTimes) {

        public static MedicationToBringBody of(MedicationToBringResponse m, List<UsableBatch> usableBatches) {
            return new MedicationToBringBody(
                    m.getPrescriptionId(),
                    m.getMedicationName(),
                    m.getDosage(),
                    m.getDosageUnit(),
                    m.getPrescriptionType(),
                    m.getMedicationSource() == null
                            ? null
                            : m.getMedicationSource().name(),
                    usableBatches.stream()
                            .map(b -> new UsableBatchBody(b.batchId(), b.expiryDate()))
                            .toList(),
                    m.getTotalQuantityNeeded(),
                    m.getScheduledTimes());
        }
    }
}
