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
 * Confirmed prescription issue record (duplicate active ingredient, allergy conflict) (FR-007, FR-008; data-model section 4).
 * Group 3 - Append-only.
 */
@Entity
@Table(name = "prescription_issue")
@DataGroup(DataGroup.Group.RECORD)
public class PrescriptionIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "reconciliation_line_id")
    private Long reconciliationLineId;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private PrescriptionIssueType issueType;

    @Column(name = "issue_code", length = 64)
    private String issueCode;

    @Column(name = "duplicate_prescription_id")
    private Long duplicatePrescriptionId;

    @Column(name = "allergy_item_id")
    private Long allergyItemId;

    @Column(name = "severity", length = 50)
    private String severity;

    @Column(name = "active_ingredient", length = 255)
    private String activeIngredient;

    @Column(name = "manual_review_required", nullable = false)
    private boolean manualReviewRequired;

    @Column(name = "confirmation_reason", length = 2000)
    private String confirmationReason;

    @Column(name = "confirmed_by_id")
    private Long confirmedById;

    @Column(name = "occurred_at", nullable = false)
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
            Instant occurredAt) {
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
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
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
