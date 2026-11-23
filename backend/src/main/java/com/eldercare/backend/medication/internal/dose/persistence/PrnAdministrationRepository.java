package com.eldercare.backend.medication.internal.dose.persistence;

import com.eldercare.backend.medication.internal.dose.domain.PrnAdministration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrnAdministrationRepository extends JpaRepository<PrnAdministration, Long> {

    List<PrnAdministration> findByPrescriptionIdOrderByAdministeredAtDesc(Long prescriptionId);

    List<PrnAdministration> findByPrescriptionIdInOrderByAdministeredAtDesc(Collection<Long> prescriptionIds);

    @Query("SELECT p FROM PrnAdministration p WHERE p.prescriptionId IN :prescriptionIds "
            + "AND p.administeredAt >= :fromTime ORDER BY p.administeredAt DESC")
    List<PrnAdministration> findInWindow(
            @Param("prescriptionIds") Collection<Long> prescriptionIds, @Param("fromTime") Instant fromTime);

    /** Lần dùng đã quá cutoff mà chưa ghi hiệu quả (BR-M07-04); since giới hạn phạm vi quét. */
    @Query("SELECT p FROM PrnAdministration p WHERE p.administeredAt <= :cutoff AND p.administeredAt >= :since "
            + "AND NOT EXISTS (SELECT 1 FROM PrnEffectiveness e WHERE e.doseResultId = p.id) ORDER BY p.id")
    List<PrnAdministration> findAwaitingEffectiveness(@Param("cutoff") Instant cutoff, @Param("since") Instant since);

    @Query("SELECT p FROM PrnAdministration p WHERE p.residentId = :residentId "
            + "AND p.administeredAt >= :fromTime ORDER BY p.administeredAt DESC")
    List<PrnAdministration> findByResidentIdInWindow(
            @Param("residentId") Long residentId, @Param("fromTime") Instant fromTime);
}
