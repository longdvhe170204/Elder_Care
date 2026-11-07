package com.eldercare.backend.medication.internal.handover.persistence;

import com.eldercare.backend.medication.internal.handover.domain.HandoverStatus;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandover;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationHandoverRepository extends JpaRepository<MedicationHandover, Long> {

    List<MedicationHandover> findByResidentId(Long residentId);

    List<MedicationHandover> findByResidentIdAndStatusIn(Long residentId, Collection<HandoverStatus> statuses);

    List<MedicationHandover> findByTripId(Long tripId);
    /** Lần giao chứa liều mang theo (chỉ mục nhiều giá trị `idx_handover_dose_ids`, `V006_97`). */
    @Query(
            value = "SELECT * FROM medication_handover WHERE kind = 'HANDOVER'"
                    + " AND CAST(:doseId AS UNSIGNED) MEMBER OF (`lines`->'$.doses[*].doseId')",
            nativeQuery = true)
    Optional<MedicationHandover> findByDoseId(@Param("doseId") Long doseId);
}
