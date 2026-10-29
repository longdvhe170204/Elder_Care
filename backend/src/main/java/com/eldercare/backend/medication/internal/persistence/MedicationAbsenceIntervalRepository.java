package com.eldercare.backend.medication.internal.persistence;

import com.eldercare.backend.medication.internal.domain.MedicationAbsenceInterval;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationAbsenceIntervalRepository extends JpaRepository<MedicationAbsenceInterval, Long> {

    List<MedicationAbsenceInterval> findByResidentId(Long residentId);

    @Query("SELECT a FROM MedicationAbsenceInterval a WHERE a.residentId = :residentId AND a.actualToTime IS NULL")
    Optional<MedicationAbsenceInterval> findOpenByResidentId(@Param("residentId") Long residentId);

    @Query("SELECT a FROM MedicationAbsenceInterval a WHERE a.residentId = :residentId "
            + "AND a.fromTime <= :toTime AND (a.expectedToTime IS NULL OR a.expectedToTime >= :fromTime)")
    List<MedicationAbsenceInterval> findOverlapping(
            @Param("residentId") Long residentId, @Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    Optional<MedicationAbsenceInterval> findByAbsenceSessionId(Long absenceSessionId);

    Optional<MedicationAbsenceInterval> findByTripId(Long tripId);
}
