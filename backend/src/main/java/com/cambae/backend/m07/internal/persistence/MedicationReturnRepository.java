package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.MedicationReturn;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationReturnRepository extends JpaRepository<MedicationReturn, Long> {

    Optional<MedicationReturn> findByHandoverId(Long handoverId);
}
