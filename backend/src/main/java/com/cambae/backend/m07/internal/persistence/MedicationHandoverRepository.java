package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.HandoverStatus;
import com.cambae.backend.m07.internal.domain.MedicationHandover;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationHandoverRepository extends JpaRepository<MedicationHandover, Long> {

    List<MedicationHandover> findByResidentId(Long residentId);

    List<MedicationHandover> findByResidentIdAndStatusIn(Long residentId, Collection<HandoverStatus> statuses);

    List<MedicationHandover> findByTripId(Long tripId);
}
