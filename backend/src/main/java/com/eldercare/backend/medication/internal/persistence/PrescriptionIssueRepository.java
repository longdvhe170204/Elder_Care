package com.eldercare.backend.medication.internal.persistence;

import com.eldercare.backend.medication.internal.domain.PrescriptionIssue;
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
