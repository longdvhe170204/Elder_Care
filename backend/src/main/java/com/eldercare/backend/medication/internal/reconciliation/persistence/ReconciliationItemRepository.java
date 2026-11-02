package com.eldercare.backend.medication.internal.reconciliation.persistence;

import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationItem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Truy cập dòng đối chiếu được nhúng trong {@code medication_reconciliation.lines}. */
@Repository
public class ReconciliationItemRepository {

    private final MedicationReconciliationRepository reconciliations;

    public ReconciliationItemRepository(MedicationReconciliationRepository reconciliations) {
        this.reconciliations = reconciliations;
    }

    public List<ReconciliationItem> findByReconciliationIdOrderByItemOrderAsc(Long reconciliationId) {
        return reconciliations
                .findById(reconciliationId)
                .map(MedicationReconciliation::getLines)
                .orElseGet(List::of);
    }

    public void deleteByReconciliationId(Long reconciliationId) {
        reconciliations.findById(reconciliationId).ifPresent(ticket -> {
            ticket.replaceLines(List.of());
            reconciliations.save(ticket);
        });
    }

    public ReconciliationItem save(ReconciliationItem item) {
        MedicationReconciliation ticket = reconciliations
                .findById(item.getReconciliationId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đối chiếu"));
        List<ReconciliationItem> lines = new ArrayList<>(ticket.getLines());
        if (item.getId() == null) {
            long nextId = lines.stream()
                            .map(ReconciliationItem::getId)
                            .filter(java.util.Objects::nonNull)
                            .mapToLong(Long::longValue)
                            .max()
                            .orElse(0L)
                    + 1;
            item.assignId(nextId);
        }
        lines.removeIf(existing -> existing.getId() != null && existing.getId().equals(item.getId()));
        lines.add(item);
        ticket.replaceLines(lines);
        reconciliations.save(ticket);
        return item;
    }
}
