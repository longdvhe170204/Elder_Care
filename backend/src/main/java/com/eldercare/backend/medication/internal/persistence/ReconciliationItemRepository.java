package com.eldercare.backend.medication.internal.persistence;

import com.eldercare.backend.medication.internal.domain.ReconciliationItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationItemRepository extends JpaRepository<ReconciliationItem, Long> {

    List<ReconciliationItem> findByReconciliationIdOrderByItemOrderAsc(Long reconciliationId);

    void deleteByReconciliationId(Long reconciliationId);
}
