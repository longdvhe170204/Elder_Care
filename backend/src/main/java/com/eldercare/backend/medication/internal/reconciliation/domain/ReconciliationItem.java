package com.eldercare.backend.medication.internal.reconciliation.domain;

import java.time.Instant;

/**
 * Medication reconciliation item line entity (FR-036).
 */
public class ReconciliationItem {

    private Long id;

    private Long reconciliationId;

    private int itemOrder;

    private ReconciliationItemSource source;

    private Long prescriptionId;

    private Long admissionMedicationItemId;

    private String medicationDescription;

    private ReconciliationDecision decision;

    private String reason;

    private String newPrescriptionJson;

    private Long createdById;

    private Instant createdAt;

    private boolean discontinuedOutsideTicket = false;

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

    public void assignId(Long id) {
        if (this.id == null) {
            this.id = id;
        }
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
