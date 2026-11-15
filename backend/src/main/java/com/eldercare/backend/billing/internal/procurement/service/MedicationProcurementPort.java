package com.eldercare.backend.billing.internal.procurement.service;

import com.eldercare.backend.medication.api.spi.ProcurementMedicationLineView;
import com.eldercare.backend.medication.api.spi.ProcurementMedicationPort;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@code medication.api.spi.ProcurementMedicationPort} của 006 (FR-054, BR-M07-24) bằng {@link ProcurementMedicationLines}
 * (019 T024; 2026-11-11, A thay D). {@code billing} đã phụ thuộc {@code medication}, nên 006 không đọc
 * {@code billing.api.ProcurementMedicationLineApi} trực tiếp (vòng module); M11 cài SPI do M07 khai như hướng ghi ở SPI.
 */
@Component
public class MedicationProcurementPort implements ProcurementMedicationPort {

    private final ProcurementMedicationLines lines;

    public MedicationProcurementPort(ProcurementMedicationLines lines) {
        this.lines = lines;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcurementMedicationLineView> lineRecordedPurchased(long lineId) {
        return lines.lineRecordedPurchased(lineId)
                .map(v -> new ProcurementMedicationLineView(
                        v.procurementLineId(),
                        v.procurementRequestId(),
                        v.residentId(),
                        v.prescriptionId(),
                        v.medicationName(),
                        v.activeIngredient(),
                        v.strength(),
                        v.dosageForm(),
                        v.quantity(),
                        v.unit(),
                        v.creatorStaffId(),
                        v.departmentId()));
    }
}
