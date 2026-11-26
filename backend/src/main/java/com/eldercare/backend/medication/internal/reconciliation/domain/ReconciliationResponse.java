package com.eldercare.backend.medication.internal.reconciliation.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Medication reconciliation response (FR-034 -> FR-038).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReconciliationResponse {

    private Long id;
    private Long residentId;
    private String reason;
    private String reasonDetail;
    private String facilityName;
    private String prescriberName;
    private java.time.LocalDate prescribedAt;
    private String comparisonSource;
    private Long attachmentFileId;
    private String attachmentName;
    private String nurseNote;
    private String doctorConclusion;
    private String status;
    private Instant deadlineAt;
    private Boolean overdue;
    private String createdBy;
    private String submittedBy;
    private String confirmedBy;
    private String returnReason;
    private String cancelReason;
    private List<ReconciliationItemResponse> items = new ArrayList<>();
    private List<String> permittedActions = new ArrayList<>();
    private List<ReconciliationCorrectionResponse> corrections = new ArrayList<>();
    private int version;

    /** Đề nghị đính chính của phiếu, mọi trạng thái, mới nhất trước (FR-039; 006 T076). */
    public List<ReconciliationCorrectionResponse> getCorrections() {
        return corrections;
    }

    public void setCorrections(List<ReconciliationCorrectionResponse> corrections) {
        this.corrections = corrections != null ? corrections : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Instant getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(Instant deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public Boolean getOverdue() {
        return overdue;
    }

    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public List<ReconciliationItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ReconciliationItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public List<String> getPermittedActions() {
        return permittedActions;
    }

    public void setPermittedActions(List<String> permittedActions) {
        this.permittedActions = permittedActions != null ? permittedActions : new ArrayList<>();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public java.time.LocalDate getPrescribedAt() {
        return prescribedAt;
    }

    public void setPrescribedAt(java.time.LocalDate prescribedAt) {
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
