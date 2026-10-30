package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.PrnAdministration;
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

    @Query("SELECT p FROM PrnAdministration p WHERE p.residentId = :residentId "
            + "AND p.administeredAt >= :fromTime ORDER BY p.administeredAt DESC")
    List<PrnAdministration> findByResidentIdInWindow(
            @Param("residentId") Long residentId, @Param("fromTime") Instant fromTime);
}
