package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Prescription repository (table prescription).
 */
@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByResidentId(Long residentId);

    List<Prescription> findByResidentIdAndStatusIn(Long residentId, Collection<PrescriptionStatus> statuses);

    @Query(
            "SELECT p FROM Prescription p WHERE p.residentId = :residentId AND p.status IN ('ACTIVE', 'SUSPENDED', 'PENDING_EFFECTIVE')")
    List<Prescription> findUnendedByResidentId(@Param("residentId") Long residentId);

    @Query(
            "SELECT p FROM Prescription p WHERE p.residentId = :residentId AND p.medicationSource = 'FACILITY' AND p.status IN ('ACTIVE', 'SUSPENDED', 'PENDING_EFFECTIVE')")
    List<Prescription> findUnendedFacilityByResidentId(@Param("residentId") Long residentId);

    @Query("SELECT p FROM Prescription p WHERE p.residentId = :residentId ORDER BY p.startedAt ASC, p.id ASC")
    List<Prescription> findHistoryByResidentId(@Param("residentId") Long residentId);

    Optional<Prescription> findByReplacesId(Long replacesId);

    List<Prescription> findByStatus(PrescriptionStatus status);

    List<Prescription> findByStatusAndStartedAtLessThanEqual(PrescriptionStatus status, java.time.Instant now);

    List<Prescription> findByStatusAndSuspendedFromLessThanEqual(PrescriptionStatus status, java.time.Instant now);

    List<Prescription> findByStatusAndEndedAtLessThanEqual(PrescriptionStatus status, java.time.LocalDate today);

    @Query("SELECT p FROM Prescription p WHERE p.status IN ('ACTIVE', 'SUSPENDED') AND p.endedAt < :today")
    List<Prescription> findExpiredPrescriptions(@Param("today") java.time.LocalDate today);

    @Query(
            "SELECT p FROM Prescription p WHERE p.status IN ('ACTIVE', 'SUSPENDED') AND p.endedAt IS NOT NULL AND p.endedAt <= :warningDate AND p.expirationWarnedAt IS NULL")
    List<Prescription> findExpiringPrescriptionsToWarn(@Param("warningDate") java.time.LocalDate warningDate);
}
