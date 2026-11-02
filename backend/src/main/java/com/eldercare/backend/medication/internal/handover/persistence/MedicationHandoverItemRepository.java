package com.eldercare.backend.medication.internal.handover.persistence;

import com.eldercare.backend.medication.internal.handover.domain.MedicationHandoverItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationHandoverItemRepository extends JpaRepository<MedicationHandoverItem, Long> {

    List<MedicationHandoverItem> findByHandoverId(Long handoverId);
}
