package com.eldercare.backend.medication.internal.reconciliation.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Medication reconciliation item line entity (FR-036). Dòng lưu trong cột JSON `lines` của phiếu: so sánh theo giá trị
 * để Hibernate không coi phiếu là bẩn mỗi lần đọc (phiếu Đã xác nhận bị trigger chặn UPDATE; FR-034a).
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReconciliationItem that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(reconciliationId, that.reconciliationId)
                && itemOrder == that.itemOrder
                && source == that.source
                && Objects.equals(prescriptionId, that.prescriptionId)
                && Objects.equals(admissionMedicationItemId, that.admissionMedicationItemId)
                && Objects.equals(medicationDescription, that.medicationDescription)
                && decision == that.decision
                && Objects.equals(reason, that.reason)
                && Objects.equals(newPrescriptionJson, that.newPrescriptionJson)
                && Objects.equals(createdById, that.createdById)
                && Objects.equals(createdAt, that.createdAt)
                && discontinuedOutsideTicket == that.discontinuedOutsideTicket
                && Objects.equals(createdPrescriptionId, that.createdPrescriptionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                reconciliationId,
                itemOrder,
                source,
                prescriptionId,
                admissionMedicationItemId,
                medicationDescription,
                decision,
                reason,
                newPrescriptionJson,
                createdById,
                createdAt,
                discontinuedOutsideTicket,
                createdPrescriptionId);
    }
}
