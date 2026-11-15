package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.billing.internal.procurement.service.MedicationLinesAwaitingIntake;
import java.math.BigDecimal;

/** api-019 {@code ProcurementMedicationLine} (Q-410): chỉ thông tin thuốc của dòng; {@code id} gửi ở requestLineId. */
public record ProcurementMedicationLineResponse(
        long id,
        long procurementRequestId,
        long residentId,
        Long prescriptionId,
        String medicationName,
        String activeIngredient,
        String strength,
        String dosageForm,
        BigDecimal quantity,
        String unit) {

    public static ProcurementMedicationLineResponse of(MedicationLinesAwaitingIntake.Line l) {
        return new ProcurementMedicationLineResponse(
                l.id(),
                l.procurementRequestId(),
                l.residentId(),
                l.prescriptionId(),
                l.medicationName(),
                l.activeIngredient(),
                l.strength(),
                l.dosageForm(),
                l.quantity(),
                l.unit());
    }
}
