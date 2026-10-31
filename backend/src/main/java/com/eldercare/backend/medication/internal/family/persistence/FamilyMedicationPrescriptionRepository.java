package com.eldercare.backend.medication.internal.family.persistence;

import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatch;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationPrescription;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/** Truy cập liên kết đơn được nhúng trong {@code family_medication_batch.prescription_bindings}. */
@Repository
public class FamilyMedicationPrescriptionRepository {

    private final FamilyMedicationBatchRepository batches;

    public FamilyMedicationPrescriptionRepository(FamilyMedicationBatchRepository batches) {
        this.batches = batches;
    }

    public List<FamilyMedicationPrescription> findByBatchId(Long batchId) {
        return batches.findById(batchId)
                .map(FamilyMedicationBatch::getPrescriptionBindings)
                .orElseGet(List::of);
    }

    public List<FamilyMedicationPrescription> findByBatchIdAndBoundToIsNull(Long batchId) {
        return findByBatchId(batchId).stream()
                .filter(binding -> binding.getBoundTo() == null)
                .toList();
    }

    public List<FamilyMedicationPrescription> findByPrescriptionId(Long prescriptionId) {
        return batches.findAll().stream()
                .flatMap(batch -> batch.getPrescriptionBindings().stream())
                .filter(binding -> Objects.equals(binding.getPrescriptionId(), prescriptionId))
                .toList();
    }

    public List<FamilyMedicationPrescription> findByPrescriptionIdAndBoundToIsNull(Long prescriptionId) {
        return findByPrescriptionId(prescriptionId).stream()
                .filter(binding -> binding.getBoundTo() == null)
                .toList();
    }

    public FamilyMedicationPrescription save(FamilyMedicationPrescription binding) {
        FamilyMedicationBatch batch = batches.findById(binding.getBatchId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô thuốc gia đình gửi"));
        List<FamilyMedicationPrescription> bindings = new ArrayList<>(batch.getPrescriptionBindings());
        if (binding.getId() == null) {
            long nextId = bindings.stream()
                            .map(FamilyMedicationPrescription::getId)
                            .filter(Objects::nonNull)
                            .mapToLong(Long::longValue)
                            .max()
                            .orElse(0L)
                    + 1;
            binding.assignId(nextId);
        }
        bindings.removeIf(
                existing -> existing.getId() != null && existing.getId().equals(binding.getId()));
        bindings.add(binding);
        batch.replacePrescriptionBindings(bindings);
        batches.save(batch);
        return binding;
    }
}
