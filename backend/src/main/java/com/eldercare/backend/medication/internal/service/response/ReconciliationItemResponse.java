package com.eldercare.backend.medication.internal.service.response;

import com.eldercare.backend.medication.internal.service.request.CreatePrescriptionBody;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Medication reconciliation line item response (FR-036).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReconciliationItemResponse {

    private Long id;
    private String source;
    private Long prescriptionId;
    private Long admissionMedicationItemId;
    private String medicationDescription;
    private String decision;
    private String reason;
    private CreatePrescriptionBody newPrescription;
    private Boolean discontinuedOutsideTicket;
    private Boolean medicationRemovedFromProfile;
    private Long createdPrescriptionId;

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

    public Boolean getDiscontinuedOutsideTicket() {
        return discontinuedOutsideTicket;
    }

    public void setDiscontinuedOutsideTicket(Boolean discontinuedOutsideTicket) {
        this.discontinuedOutsideTicket = discontinuedOutsideTicket;
    }

    public Boolean getMedicationRemovedFromProfile() {
        return medicationRemovedFromProfile;
    }

    public void setMedicationRemovedFromProfile(Boolean medicationRemovedFromProfile) {
        this.medicationRemovedFromProfile = medicationRemovedFromProfile;
    }

    public Long getCreatedPrescriptionId() {
        return createdPrescriptionId;
    }

    public void setCreatedPrescriptionId(Long createdPrescriptionId) {
        this.createdPrescriptionId = createdPrescriptionId;
    }
}
