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
 * Carried medication return item line (FR-048a).
 */
@Entity
@DataGroup(DataGroup.Group.RECORD)
@Table(name = "medication_return_item")
public class MedicationReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_id", nullable = false)
    private Long returnId;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    protected MedicationReturnItem() {}

    public MedicationReturnItem(Long returnId, Long prescriptionId, BigDecimal quantity) {
        this.returnId = returnId;
        this.prescriptionId = prescriptionId;
        this.quantity = quantity;
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
}
