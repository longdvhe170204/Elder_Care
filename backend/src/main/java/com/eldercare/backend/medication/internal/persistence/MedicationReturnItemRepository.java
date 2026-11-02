package com.eldercare.backend.medication.internal.persistence;

import com.eldercare.backend.medication.internal.domain.MedicationReturnItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationReturnItemRepository extends JpaRepository<MedicationReturnItem, Long> {

    List<MedicationReturnItem> findByReturnId(Long returnId);
}
