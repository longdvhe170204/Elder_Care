package com.eldercare.backend.medication.internal.handover.persistence;

import com.eldercare.backend.medication.internal.handover.domain.MedicationHandoverDose;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationHandoverDoseRepository extends JpaRepository<MedicationHandoverDose, Long> {

    List<MedicationHandoverDose> findByHandoverId(Long handoverId);

    Optional<MedicationHandoverDose> findByDoseId(Long doseId);
}
