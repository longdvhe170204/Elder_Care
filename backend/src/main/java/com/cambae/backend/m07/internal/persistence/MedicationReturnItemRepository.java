package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.MedicationReturnItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationReturnItemRepository extends JpaRepository<MedicationReturnItem, Long> {

    List<MedicationReturnItem> findByReturnId(Long returnId);
}
