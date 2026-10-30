package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.MedicationHandoverItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationHandoverItemRepository extends JpaRepository<MedicationHandoverItem, Long> {

    List<MedicationHandoverItem> findByHandoverId(Long handoverId);
}
