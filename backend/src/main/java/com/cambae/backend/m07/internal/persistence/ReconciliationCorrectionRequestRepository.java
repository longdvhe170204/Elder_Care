package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.ReconciliationCorrectionRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationCorrectionRequestRepository
        extends JpaRepository<ReconciliationCorrectionRequest, Long> {

    List<ReconciliationCorrectionRequest> findByReconciliationId(Long reconciliationId);
}
