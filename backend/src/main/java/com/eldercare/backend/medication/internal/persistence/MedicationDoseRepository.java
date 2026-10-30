package com.eldercare.backend.medication.internal.persistence;

import com.eldercare.backend.medication.internal.domain.DoseStatus;
import com.eldercare.backend.medication.internal.domain.MedicationDose;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho {@link MedicationDose}.
 */
@Repository
public interface MedicationDoseRepository extends JpaRepository<MedicationDose, Long> {

    Optional<MedicationDose> findByPrescriptionIdAndScheduledTime(Long prescriptionId, Instant scheduledTime);

    List<MedicationDose> findByResidentIdAndScheduledTimeBetween(Long residentId, Instant fromTime, Instant toTime);

    List<MedicationDose> findByStatusAndWindowFromLessThanEqual(DoseStatus status, Instant now);

    List<MedicationDose> findByStatusAndWindowToLessThanEqual(DoseStatus status, Instant now);

    List<MedicationDose> findByStatusAndLateAtLessThanEqual(DoseStatus status, Instant now);

    List<MedicationDose> findByStatusAndEndedAtIsNullAndWindowToLessThanEqual(DoseStatus status, Instant now);

    List<MedicationDose> findByResidentIdAndMedicationIdOrderByScheduledTimeDesc(Long residentId, Long medicationId);

    List<MedicationDose> findByPrescriptionIdAndStatusIn(Long prescriptionId, Collection<DoseStatus> statuses);

    List<MedicationDose> findByFloorAreaIdAndScheduledTimeBetween(Long floorAreaId, Instant fromTime, Instant toTime);

    List<MedicationDose> findByFloorAreaIdAndStatusIn(Long floorAreaId, Collection<DoseStatus> statuses);
}
