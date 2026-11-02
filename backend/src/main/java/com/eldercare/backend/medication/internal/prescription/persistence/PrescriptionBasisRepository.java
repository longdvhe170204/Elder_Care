package com.eldercare.backend.medication.internal.prescription.persistence;

import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionBasis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Prescription basis repository (table prescription_basis).
 */
@Repository
public interface PrescriptionBasisRepository extends JpaRepository<PrescriptionBasis, Long> {

    List<PrescriptionBasis> findByPrescriptionId(Long prescriptionId);
}
