package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.FamilyMedicationPrescription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyMedicationPrescriptionRepository extends JpaRepository<FamilyMedicationPrescription, Long> {
    List<FamilyMedicationPrescription> findByBatchId(Long batchId);

    List<FamilyMedicationPrescription> findByBatchIdAndBoundToIsNull(Long batchId);

    List<FamilyMedicationPrescription> findByPrescriptionId(Long prescriptionId);

    List<FamilyMedicationPrescription> findByPrescriptionIdAndBoundToIsNull(Long prescriptionId);
}
