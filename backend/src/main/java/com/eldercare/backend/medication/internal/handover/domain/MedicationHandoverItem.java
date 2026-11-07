package com.eldercare.backend.medication.internal.handover.domain;

import java.math.BigDecimal;

/** Dòng thuốc giao, nhúng trong {@code medication_handover.lines.items} (006 T069, {@code V006_97}). */
public class MedicationHandoverItem {

    private Long id;

    private Long handoverId;

    private Long prescriptionId;

    private BigDecimal quantity;

    private Long familyMedicationBatchId;

    private String medicationSource;

    public MedicationHandoverItem() {}

    public MedicationHandoverItem(
            Long handoverId,
            Long prescriptionId,
            BigDecimal quantity,
            Long familyMedicationBatchId,
            String medicationSource) {
        this.handoverId = handoverId;
        this.prescriptionId = prescriptionId;
        this.quantity = quantity;
        this.familyMedicationBatchId = familyMedicationBatchId;
        this.medicationSource = medicationSource;
    }

    public void assignId(Long id) {
        if (this.id == null) {
            this.id = id;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getHandoverId() {
        return handoverId;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Long getFamilyMedicationBatchId() {
        return familyMedicationBatchId;
    }

    public String getMedicationSource() {
        return medicationSource;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MedicationHandoverItem other
                && java.util.Objects.equals(id, other.id)
                && java.util.Objects.equals(handoverId, other.handoverId)
                && java.util.Objects.equals(prescriptionId, other.prescriptionId)
                && java.util.Objects.equals(quantity, other.quantity)
                && java.util.Objects.equals(familyMedicationBatchId, other.familyMedicationBatchId)
                && java.util.Objects.equals(medicationSource, other.medicationSource);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                id, handoverId, prescriptionId, quantity, familyMedicationBatchId, medicationSource);
    }
}
