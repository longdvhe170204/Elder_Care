package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.PrescriptionIssue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Prescription issue repository (table prescription_issue).
 */
@Repository
public interface PrescriptionIssueRepository extends JpaRepository<PrescriptionIssue, Long> {

    List<PrescriptionIssue> findByPrescriptionId(Long prescriptionId);

    List<PrescriptionIssue> findByReconciliationLineId(Long reconciliationLineId);
}
