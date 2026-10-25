package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.DoseResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho {@link DoseResult} (Group 3 append-only).
 */
@Repository
public interface DoseResultRepository extends JpaRepository<DoseResult, Long> {

    Optional<DoseResult> findByDoseId(Long doseId);

    List<DoseResult> findByResidentIdOrderByRecordedAtDesc(Long residentId);
}
