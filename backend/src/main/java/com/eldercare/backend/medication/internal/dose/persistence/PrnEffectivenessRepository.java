package com.eldercare.backend.medication.internal.dose.persistence;

import com.eldercare.backend.medication.internal.dose.domain.PrnEffectiveness;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrnEffectivenessRepository extends JpaRepository<PrnEffectiveness, Long> {

    Optional<PrnEffectiveness> findByDoseResultId(Long doseResultId);

    List<PrnEffectiveness> findByDoseResultIdIn(Collection<Long> doseResultIds);
}
