package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;

/**
 * Item body for medication reconciliation sheet (FR-036).
 */
public class ReconciliationItemBody {

    private Long id;

    @NotEmpty
    @JsonAlias({"nguon", "source"})
    private String source;

    @JsonAlias({"donThuocId", "prescriptionId"})
    private Long prescriptionId;

    @JsonAlias({"mucThuocDangDungId", "admissionMedicationItemId"})
    private Long admissionMedicationItemId;

    @JsonAlias({"moTaThuoc", "medicationDescription"})
    private String medicationDescription;

    @JsonAlias({"quyetDinh", "decision"})
    private String decision;

    @JsonAlias({"lyDo", "reason"})
    private String reason;

    @JsonAlias({"donMoi", "newPrescription"})
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
}
