package com.eldercare.backend.medication.internal.reconciliation.domain;

import com.eldercare.backend.medication.internal.prescription.domain.CreatePrescriptionBody;
import jakarta.validation.constraints.NotEmpty;

/**
 * Item body for medication reconciliation sheet (FR-036).
 */
public class ReconciliationItemBody {

    private Long id;

    @NotEmpty
    private String source;

    private Long prescriptionId;

    private Long admissionMedicationItemId;

    private String medicationDescription;

    private String decision;

    private String nurseFinding;

    private String comparisonSummary;

    private java.time.Instant lastHospitalDoseAt;

    private String reason;

    private CreatePrescriptionBody newPrescription;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public Long getAdmissionMedicationItemId() {
        return admissionMedicationItemId;
    }

    public void setAdmissionMedicationItemId(Long admissionMedicationItemId) {
        this.admissionMedicationItemId = admissionMedicationItemId;
    }

    public String getMedicationDescription() {
        return medicationDescription;
    }

    public void setMedicationDescription(String medicationDescription) {
        this.medicationDescription = medicationDescription;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public CreatePrescriptionBody getNewPrescription() {
        return newPrescription;
    }

    public void setNewPrescription(CreatePrescriptionBody newPrescription) {
        this.newPrescription = newPrescription;
    }

    public String getNurseFinding() {
        return nurseFinding;
    }

    public void setNurseFinding(String nurseFinding) {
        this.nurseFinding = nurseFinding;
    }

    public String getComparisonSummary() {
        return comparisonSummary;
    }

    public void setComparisonSummary(String comparisonSummary) {
        this.comparisonSummary = comparisonSummary;
    }

    public java.time.Instant getLastHospitalDoseAt() {
        return lastHospitalDoseAt;
    }

    public void setLastHospitalDoseAt(java.time.Instant lastHospitalDoseAt) {
        this.lastHospitalDoseAt = lastHospitalDoseAt;
    }
}
