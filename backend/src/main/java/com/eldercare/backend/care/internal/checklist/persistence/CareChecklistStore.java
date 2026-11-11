package com.eldercare.backend.care.internal.checklist.persistence;

import com.eldercare.backend.care.internal.careplan.persistence.CareRiskStore;
import com.eldercare.backend.care.internal.checklist.domain.ChecklistRiskAgreement;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CareChecklistStore {
    private final CareRiskStore risks;

    public CareChecklistStore(CareRiskStore risks) {
        this.risks = risks;
    }

    /** Thỏa thuận rủi ro của kế hoạch Hiệu lực tại {@code date} của từng người cao tuổi (FR-030). */
    public List<ChecklistRiskAgreement> agreements(Collection<Long> residents, LocalDate date) {
        if (residents.isEmpty()) return List.of();
        return risks.effectiveByResident(residents, date).entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(a -> new ChecklistRiskAgreement(e.getKey(), a)))
                .toList();
    }
}
