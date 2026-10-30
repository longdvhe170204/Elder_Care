package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.SuppliedMedicationIntake;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuppliedMedicationIntakeRepository extends JpaRepository<SuppliedMedicationIntake, Long> {
    List<SuppliedMedicationIntake> findByProcurementRequestId(Long procurementRequestId);

    List<SuppliedMedicationIntake> findByProcurementLineId(Long procurementLineId);

    List<SuppliedMedicationIntake> findByResidentId(Long residentId);
}
