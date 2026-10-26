package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.FamilyMedicationMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyMedicationMovementRepository extends JpaRepository<FamilyMedicationMovement, Long> {
    List<FamilyMedicationMovement> findByBatchIdOrderByMovedAtDesc(Long batchId);
}
