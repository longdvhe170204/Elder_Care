package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.NoMedicationOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoMedicationOrderRepository extends JpaRepository<NoMedicationOrder, Long> {

    Optional<NoMedicationOrder> findByResidentIdAndTripId(Long residentId, Long tripId);

    List<NoMedicationOrder> findByResidentId(Long residentId);
}
