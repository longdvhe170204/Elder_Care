package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.care.internal.careplan.persistence.CareRiskStore;
import com.eldercare.backend.resident.api.spi.CareRiskAgreementPort;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link CareRiskAgreementPort} của 001 (FR-036b, Q-423) bằng thỏa thuận đang dùng của kế hoạch Hiệu lực, như checklist
 * ca (FR-030).
 */
@Service
@Transactional(readOnly = true)
class CareRiskAgreementPortImpl implements CareRiskAgreementPort {

    private final CareRiskStore risks;

    CareRiskAgreementPortImpl(CareRiskStore risks) {
        this.risks = risks;
    }

    @Override
    public boolean isActive(long residentId, long agreementId, LocalDate at) {
        return risks.effectiveByResident(List.of(residentId), at).getOrDefault(residentId, List.of()).stream()
                .anyMatch(a -> a.id() == agreementId || a.lineage() == agreementId);
    }
}
