package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.MedicationReconciliation;
import com.cambae.backend.m07.internal.domain.ReconciliationStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationReconciliationRepository extends JpaRepository<MedicationReconciliation, Long> {

    List<MedicationReconciliation> findByResidentId(Long residentId);

    List<MedicationReconciliation> findByResidentIdAndStatusIn(
            Long residentId, Collection<ReconciliationStatus> statuses);

    @Query(
            "SELECT r FROM MedicationReconciliation r WHERE r.residentId = :residentId AND r.status IN ('OPEN', 'PENDING_CONFIRMATION')")
    Optional<MedicationReconciliation> findOpenOrPendingByResidentId(@Param("residentId") Long residentId);

    @Query(
            "SELECT r FROM MedicationReconciliation r WHERE r.status IN ('OPEN', 'PENDING_CONFIRMATION') AND r.deadlineAt IS NOT NULL AND r.deadlineAt <= :now AND r.overdueAt IS NULL")
    List<MedicationReconciliation> findOverdueTickets(@Param("now") Instant now);
}
