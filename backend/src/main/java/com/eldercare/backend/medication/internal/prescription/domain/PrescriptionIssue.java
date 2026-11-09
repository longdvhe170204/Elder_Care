package com.eldercare.backend.medication.internal.prescription.domain;

import java.time.Instant;

/** Dòng {@code record_event} loại {@code M07.PRESCRIPTION_ISSUE}, đối tượng {@code M07.PRESCRIPTION} (006 T069, {@code V006_98}). */
public class PrescriptionIssue {

    private Long id;

    private Long prescriptionId;

    private Long reconciliationLineId;

    private Integer lineNumber;

    private PrescriptionIssueType issueType;

    private String issueCode;

    private Long duplicatePrescriptionId;

    private Long allergyItemId;

    private String severity;

    private String activeIngredient;

    private boolean manualReviewRequired;

    private String confirmationReason;

    private Long confirmedById;

    private Instant occurredAt;

    protected PrescriptionIssue() {}

    public PrescriptionIssue(
            Long prescriptionId,
            Long reconciliationLineId,
            Integer lineNumber,
            PrescriptionIssueType issueType,
            String issueCode,
            Long duplicatePrescriptionId,
            Long allergyItemId,
            String severity,
            String activeIngredient,
            boolean manualReviewRequired,
            String confirmationReason,
            Long confirmedById,
            Instant occurredAt,
            Instant now) {
        this.prescriptionId = prescriptionId;
        this.reconciliationLineId = reconciliationLineId;
        this.lineNumber = lineNumber;
        this.issueType = issueType;
        this.issueCode = issueCode;
        this.duplicatePrescriptionId = duplicatePrescriptionId;
        this.allergyItemId = allergyItemId;
        this.severity = severity;
        this.activeIngredient = activeIngredient;
        this.manualReviewRequired = manualReviewRequired;
        this.confirmationReason = confirmationReason;
        this.confirmedById = confirmedById;
        this.occurredAt = occurredAt != null ? occurredAt : now;
    }

    public void assignId(long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public Long getReconciliationLineId() {
        return reconciliationLineId;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public PrescriptionIssueType getIssueType() {
        return issueType;
    }

    public String getIssueCode() {
        return issueCode;
    }

    public Long getDuplicatePrescriptionId() {
        return duplicatePrescriptionId;
    }

    public Long getAllergyItemId() {
        return allergyItemId;
    }

    public String getSeverity() {
        return severity;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public String getConfirmationReason() {
        return confirmationReason;
    }

    public Long getConfirmedById() {
        return confirmedById;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
