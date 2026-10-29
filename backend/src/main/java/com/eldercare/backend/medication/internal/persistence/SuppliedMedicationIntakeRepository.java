package com.eldercare.backend.medication.internal.persistence;

import com.eldercare.backend.medication.internal.domain.SuppliedMedicationIntake;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuppliedMedicationIntakeRepository extends JpaRepository<SuppliedMedicationIntake, Long> {
    List<SuppliedMedicationIntake> findByProcurementRequestId(Long procurementRequestId);

    List<SuppliedMedicationIntake> findByProcurementLineId(Long procurementLineId);

    List<SuppliedMedicationIntake> findByResidentId(Long residentId);
}
