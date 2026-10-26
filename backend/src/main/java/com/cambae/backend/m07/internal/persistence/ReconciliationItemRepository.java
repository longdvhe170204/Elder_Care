package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.ReconciliationItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationItemRepository extends JpaRepository<ReconciliationItem, Long> {

    List<ReconciliationItem> findByReconciliationIdOrderByItemOrderAsc(Long reconciliationId);

    void deleteByReconciliationId(Long reconciliationId);
}
