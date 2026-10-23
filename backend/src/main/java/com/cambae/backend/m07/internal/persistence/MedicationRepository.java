package com.cambae.backend.m07.internal.persistence;

import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.MedicationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Kho dữ liệu Danh mục thuốc (bảng {@code medication}).
 */
public interface MedicationRepository extends JpaRepository<Medication, Long> {

    boolean existsByNameIgnoreCaseAndConcentrationIgnoreCaseAndDosageFormIgnoreCase(
            String name, String concentration, String dosageForm);

    boolean existsByNameIgnoreCaseAndConcentrationIgnoreCaseAndDosageFormIgnoreCaseAndIdNot(
            String name, String concentration, String dosageForm, Long id);

    List<Medication> findByStatus(MedicationStatus status);

    @Query("SELECT m FROM Medication m WHERE (:status IS NULL OR m.status = :status) "
            + "AND (:q IS NULL OR :q = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(m.concentration) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(m.dosageForm) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "ORDER BY m.name ASC, m.id ASC")
    List<Medication> search(@Param("q") String q, @Param("status") MedicationStatus status);
}
