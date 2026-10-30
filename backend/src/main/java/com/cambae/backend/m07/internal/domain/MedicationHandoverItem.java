package com.cambae.backend.m07.internal.domain;

import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Carried medication line item in handover record (FR-048a).
 */
@Entity
@DataGroup(DataGroup.Group.RECORD)
@Table(name = "medication_handover_item")
public class MedicationHandoverItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "handover_id", nullable = false)
    private Long handoverId;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "family_medication_batch_id")
    private Long familyMedicationBatchId;

    @Column(name = "medication_source", length = 30)
    private String medicationSource;

    protected MedicationHandoverItem() {}

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
}
