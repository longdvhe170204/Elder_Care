package com.eldercare.backend.medication.internal.reconciliation.domain;

import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;

/**
 * Request body for creating a medication reconciliation form (FR-034, FR-034b).
 */
public class CreateReconciliationBody {

    @NotEmpty
    private String reason; // ADMISSION | HOSPITAL_RETURN | PROFESSIONAL_REQUEST

    private String reasonDetail;
    private String facilityName;
    private String prescriberName;
    private LocalDate prescribedAt;
    private String comparisonSource;
    private Long attachmentFileId;
    private String attachmentName;
    private String nurseNote;
    private String doctorConclusion;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public void setReasonDetail(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getPrescriberName() {
        return prescriberName;
    }

    public void setPrescriberName(String prescriberName) {
        this.prescriberName = prescriberName;
    }

    public LocalDate getPrescribedAt() {
        return prescribedAt;
    }

    public void setPrescribedAt(LocalDate prescribedAt) {
        this.prescribedAt = prescribedAt;
    }

    public String getComparisonSource() {
        return comparisonSource;
    }

    public void setComparisonSource(String comparisonSource) {
        this.comparisonSource = comparisonSource;
    }

    public Long getAttachmentFileId() {
        return attachmentFileId;
    }

    public void setAttachmentFileId(Long attachmentFileId) {
        this.attachmentFileId = attachmentFileId;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public String getNurseNote() {
        return nurseNote;
    }

    public void setNurseNote(String nurseNote) {
        this.nurseNote = nurseNote;
    }

    public String getDoctorConclusion() {
        return doctorConclusion;
    }

    public void setDoctorConclusion(String doctorConclusion) {
        this.doctorConclusion = doctorConclusion;
    }
}
