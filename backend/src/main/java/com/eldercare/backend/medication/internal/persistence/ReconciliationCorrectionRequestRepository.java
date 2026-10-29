package com.eldercare.backend.medication.internal.persistence;

import com.eldercare.backend.medication.internal.domain.ReconciliationCorrectionRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationCorrectionRequestRepository
        extends JpaRepository<ReconciliationCorrectionRequest, Long> {

    List<ReconciliationCorrectionRequest> findByReconciliationId(Long reconciliationId);
}
