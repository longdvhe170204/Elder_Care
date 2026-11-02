package com.eldercare.backend.medication.internal.handover.persistence;

import com.eldercare.backend.medication.internal.handover.domain.MedicationReturnItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationReturnItemRepository extends JpaRepository<MedicationReturnItem, Long> {

    List<MedicationReturnItem> findByReturnId(Long returnId);
}
