package com.eldercare.backend.medication.internal.handover.domain;

import java.math.BigDecimal;

/** Dòng thuốc nhận lại, nhúng trong {@code lines.items} của dòng {@code RETURN} (006 T069, {@code V006_97}). */
public class MedicationReturnItem {

    private Long id;

    private Long returnId;

    private Long prescriptionId;

    private BigDecimal quantity;

    public MedicationReturnItem() {}

    public MedicationReturnItem(Long returnId, Long prescriptionId, BigDecimal quantity) {
        this.returnId = returnId;
        this.prescriptionId = prescriptionId;
        this.quantity = quantity;
    }

    public void assignId(Long id) {
        if (this.id == null) {
            this.id = id;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getReturnId() {
        return returnId;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MedicationReturnItem other
                && java.util.Objects.equals(id, other.id)
                && java.util.Objects.equals(returnId, other.returnId)
                && java.util.Objects.equals(prescriptionId, other.prescriptionId)
                && java.util.Objects.equals(quantity, other.quantity);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, returnId, prescriptionId, quantity);
    }
}
