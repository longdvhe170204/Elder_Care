package com.eldercare.backend.medication.internal.handover.persistence;

import com.eldercare.backend.medication.internal.handover.domain.MedicationReturn;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationReturnRepository extends JpaRepository<MedicationReturn, Long> {

    Optional<MedicationReturn> findByHandoverId(Long handoverId);
}
