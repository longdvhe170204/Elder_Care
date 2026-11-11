package com.eldercare.backend.billing.internal.procurement.service;

import com.eldercare.backend.billing.api.spi.ProcurementIntakePort;
import com.eldercare.backend.medication.api.ProcurementIntakeApi;
import com.eldercare.backend.medication.api.dto.IntakeResultView;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Cài {@link ProcurementIntakePort} ("Đã nhận" của yêu cầu loại thuốc, FR-008d, Q-337) bằng
 * {@code medication.api.ProcurementIntakeApi} (019 T024; 2026-11-11, A thay D). {@code billing} đã phụ thuộc
 * {@code medication}, nên M07 không cài SPI của M11 (vòng module); cùng hướng với {@link MedicationProcurementPort}.
 */
@Component
public class MedicationIntakes implements ProcurementIntakePort {

    private final ProcurementIntakeApi intakes;

    public MedicationIntakes(ProcurementIntakeApi intakes) {
        this.intakes = intakes;
    }

    @Override
    public Set<Long> passedLineIds(long procurementRequestId) {
        return intakes.byProcurementRequest(procurementRequestId).stream()
                .filter(IntakeResultView::passed)
                .map(IntakeResultView::procurementLineId)
                .collect(Collectors.toUnmodifiableSet());
    }
}
