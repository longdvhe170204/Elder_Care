package com.eldercare.backend.billing.internal.procurement.service;

import static com.eldercare.backend.billing.api.dto.ProcurementStatus.*;

import com.eldercare.backend.billing.api.ProcurementMedicationLineApi;
import com.eldercare.backend.billing.api.dto.ProcurementMedicationLineView;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementLineId;
import com.eldercare.backend.billing.internal.procurement.persistence.ProcurementRequestStore;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T027 (FR-008d, Q-337; chủ dự án duyệt 2026-11-08): dòng thuốc đã ghi đã mua cho 006 ghi lần tiếp nhận theo dòng.
 * Bộ phận là bộ phận của yêu cầu (không đổi khi người lập chuyển bộ phận); trả lại đối chiếu đưa yêu cầu về Đã duyệt nên
 * dòng lại rỗng tới lần ghi đã mua sau.
 */
@Service
public class ProcurementMedicationLines implements ProcurementMedicationLineApi {
    private static final Set<?> PURCHASED = Set.of(PENDING_RECONCILIATION, DISCREPANCY, RECONCILED);

    private final ProcurementRequestStore store;

    public ProcurementMedicationLines(ProcurementRequestStore store) {
        this.store = store;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcurementMedicationLineView> lineRecordedPurchased(long lineId) {
        int index = ProcurementLineId.index(lineId);
        if (index < 0) return Optional.empty();
        return store.find(ProcurementLineId.requestId(lineId))
                .filter(r -> r.type() == ProcurementType.MEDICATION && PURCHASED.contains(r.status()))
                .filter(r -> index < r.lines().size())
                .map(r -> {
                    var line = r.lines().get(index);
                    return new ProcurementMedicationLineView(
                            lineId,
                            r.id(),
                            line.residentId(),
                            line.prescriptionId(),
                            line.name(),
                            line.activeIngredient(),
                            line.strength(),
                            line.dosageForm(),
                            line.quantity(),
                            line.unit(),
                            r.createdByStaffId(),
                            r.departmentId());
                });
    }
}
