package com.cambae.backend.support.fake.m07;

import com.cambae.backend.m07.api.spi.ProcurementMedicationLineView;
import com.cambae.backend.m07.api.spi.ProcurementMedicationPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bản giả {@link ProcurementMedicationPort} (Module 019 cài): trả về dòng yêu cầu thuốc mua sắm.
 */
public class FakeProcurementMedicationPort implements ProcurementMedicationPort {

    private final Map<Long, ProcurementMedicationLineView> lines = new HashMap<>();

    public FakeProcurementMedicationPort recordLine(ProcurementMedicationLineView line) {
        lines.put(line.procurementLineId(), line);
        return this;
    }

    @Override
    public Optional<ProcurementMedicationLineView> lineRecordedPurchased(long lineId) {
        return Optional.ofNullable(lines.get(lineId));
    }
}
