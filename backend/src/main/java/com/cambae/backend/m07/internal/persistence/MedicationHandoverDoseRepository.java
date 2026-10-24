package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.MedicationHandoverDose;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationHandoverDoseRepository extends JpaRepository<MedicationHandoverDose, Long> {

    List<MedicationHandoverDose> findByHandoverId(Long handoverId);

    Optional<MedicationHandoverDose> findByDoseId(Long doseId);
}
