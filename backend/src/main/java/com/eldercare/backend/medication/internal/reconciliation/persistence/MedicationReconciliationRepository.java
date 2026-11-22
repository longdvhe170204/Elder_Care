package com.eldercare.backend.medication.internal.reconciliation.persistence;

import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationReconciliationRepository extends JpaRepository<MedicationReconciliation, Long> {

    List<MedicationReconciliation> findByResidentId(Long residentId);

    List<MedicationReconciliation> findByStatusIn(Collection<ReconciliationStatus> statuses);

    List<MedicationReconciliation> findByResidentIdAndStatusIn(
            Long residentId, Collection<ReconciliationStatus> statuses);

    @Query(
            "SELECT r FROM MedicationReconciliation r WHERE r.residentId = :residentId AND r.status IN ('OPEN', 'PENDING_CONFIRMATION')")
    Optional<MedicationReconciliation> findOpenOrPendingByResidentId(@Param("residentId") Long residentId);

    @Query(
            "SELECT r FROM MedicationReconciliation r WHERE r.status IN ('OPEN', 'PENDING_CONFIRMATION') AND r.deadlineAt IS NOT NULL AND r.deadlineAt <= :now AND r.overdueAt IS NULL")
    List<MedicationReconciliation> findOverdueTickets(@Param("now") Instant now);

    /** Phiếu mở quá hạn lần hai chưa báo thêm Trưởng tầng, Quản lý viện (BR-M07-29, CFG-M07-10). */
    @Query(
            "SELECT r FROM MedicationReconciliation r WHERE r.status IN ('OPEN', 'PENDING_CONFIRMATION') AND r.deadlineAt IS NOT NULL AND r.deadlineAt <= :cutoff AND r.escalatedAt IS NULL")
    List<MedicationReconciliation> findNeedingEscalation(@Param("cutoff") Instant cutoff);
}
