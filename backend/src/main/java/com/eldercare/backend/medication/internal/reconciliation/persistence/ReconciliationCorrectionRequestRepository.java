package com.eldercare.backend.medication.internal.reconciliation.persistence;

import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationCorrectionRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationCorrectionRequestRepository
        extends JpaRepository<ReconciliationCorrectionRequest, Long> {

    List<ReconciliationCorrectionRequest> findByReconciliationId(Long reconciliationId);
}
