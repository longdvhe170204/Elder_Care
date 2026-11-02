package com.eldercare.backend.medication.internal.persistence;

import com.eldercare.backend.medication.internal.domain.FamilyMedicationBatch;
import com.eldercare.backend.medication.internal.domain.FamilyMedicationBatchStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyMedicationBatchRepository extends JpaRepository<FamilyMedicationBatch, Long> {
    List<FamilyMedicationBatch> findByResidentId(Long residentId);

    List<FamilyMedicationBatch> findByResidentIdAndStatus(Long residentId, FamilyMedicationBatchStatus status);

    List<FamilyMedicationBatch> findByResidentIdAndStatusIn(
            Long residentId, List<FamilyMedicationBatchStatus> statuses);

    List<FamilyMedicationBatch> findByResidentIdAndMedicationIdAndStatus(
            Long residentId, Long medicationId, FamilyMedicationBatchStatus status);
}
