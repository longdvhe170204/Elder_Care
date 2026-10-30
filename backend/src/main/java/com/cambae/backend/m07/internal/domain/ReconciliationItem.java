package com.cambae.backend.m07.internal.domain;

import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Medication reconciliation item line entity (FR-036).
 */
@Entity
@DataGroup(DataGroup.Group.BUSINESS)
@Table(name = "reconciliation_item")
public class ReconciliationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reconciliation_id", nullable = false)
    private Long reconciliationId;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 35)
    private ReconciliationItemSource source;

    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "admission_medication_item_id")
    private Long admissionMedicationItemId;

    @Column(name = "medication_description", length = 1000)
    private String medicationDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationDecision decision;

    @Column(length = 2000)
    private String reason;

    @Column(name = "new_prescription_json", columnDefinition = "json")
    private String newPrescriptionJson;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "discontinued_outside_ticket", nullable = false)
    private boolean discontinuedOutsideTicket = false;

    @Column(name = "created_prescription_id")
    private Long createdPrescriptionId;

    protected ReconciliationItem() {}

    public ReconciliationItem(
            Long reconciliationId,
            int itemOrder,
            ReconciliationItemSource source,
            Long prescriptionId,
            Long admissionMedicationItemId,
            String medicationDescription,
            ReconciliationDecision decision,
            String reason,
            String newPrescriptionJson,
            Long createdById,
            Instant createdAt,
            Instant now) {
        this.reconciliationId = reconciliationId;
        this.itemOrder = itemOrder;
        this.source = source;
        this.prescriptionId = prescriptionId;
        this.admissionMedicationItemId = admissionMedicationItemId;
        this.medicationDescription = medicationDescription;
        this.decision = decision;
        this.reason = reason;
        this.newPrescriptionJson = newPrescriptionJson;
        this.createdById = createdById;
        this.createdAt = createdAt != null ? createdAt : now;
    }

    public void updateDecision(ReconciliationDecision decision, String reason, String newPrescriptionJson) {
        this.decision = decision;
        this.reason = reason;
        this.newPrescriptionJson = newPrescriptionJson;
    }

    public void recordCreatedPrescription(Long createdPrescriptionId) {
        this.createdPrescriptionId = createdPrescriptionId;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public Long getReconciliationId() {
        return reconciliationId;
    }

    public int getItemOrder() {
        return itemOrder;
    }

    public ReconciliationItemSource getSource() {
        return source;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public Long getAdmissionMedicationItemId() {
        return admissionMedicationItemId;
    }

    public String getMedicationDescription() {
        return medicationDescription;
    }

    public ReconciliationDecision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public String getNewPrescriptionJson() {
        return newPrescriptionJson;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isDiscontinuedOutsideTicket() {
        return discontinuedOutsideTicket;
    }

    public Long getCreatedPrescriptionId() {
        return createdPrescriptionId;
    }
}
