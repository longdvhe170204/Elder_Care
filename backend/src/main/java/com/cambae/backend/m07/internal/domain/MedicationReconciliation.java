package com.cambae.backend.m07.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * Medication reconciliation entity (FR-034 -> FR-039; research D-13, D-16).
 */
@Entity
@Table(name = "medication_reconciliation")
public class MedicationReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReconciliationReason reason;

    @Column(name = "reason_detail", length = 2000)
    private String reasonDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReconciliationStatus status = ReconciliationStatus.OPEN;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "overdue_at")
    private Instant overdueAt;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "submitted_by_id")
    private Long submittedById;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "confirmed_by_id")
    private Long confirmedById;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "return_reason", length = 2000)
    private String returnReason;

    @Column(name = "cancel_reason", length = 2000)
    private String cancelReason;

    @Version
    @Column(nullable = false)
    private int version;

    protected MedicationReconciliation() {}

    public MedicationReconciliation(
            Long residentId,
            ReconciliationReason reason,
            String reasonDetail,
            Instant deadlineAt,
            Long createdById,
            Instant createdAt) {
        this.residentId = residentId;
        this.reason = reason;
        this.reasonDetail = reasonDetail;
        this.deadlineAt = deadlineAt;
        this.createdById = createdById;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.status = ReconciliationStatus.OPEN;
    }

    public void submit(Long submittedById, Instant submittedAt) {
        this.status = ReconciliationStatus.PENDING_CONFIRMATION;
        this.submittedById = submittedById;
        this.submittedAt = submittedAt != null ? submittedAt : Instant.now();
        this.returnReason = null;
    }

    public void returnForEdit(String reason) {
        this.status = ReconciliationStatus.OPEN;
        this.returnReason = reason;
    }

    public void confirm(Long confirmedById, Instant confirmedAt) {
        this.status = ReconciliationStatus.CONFIRMED;
        this.confirmedById = confirmedById;
        this.confirmedAt = confirmedAt != null ? confirmedAt : Instant.now();
    }

    public void cancel(String reason) {
        this.status = ReconciliationStatus.CANCELLED;
        this.cancelReason = reason;
    }

    public void markOverdue(Instant overdueAt) {
        this.overdueAt = overdueAt;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public Long getResidentId() {
        return residentId;
    }

    public ReconciliationReason getReason() {
        return reason;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public Instant getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(Instant deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public Instant getOverdueAt() {
        return overdueAt;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getSubmittedById() {
        return submittedById;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Long getConfirmedById() {
        return confirmedById;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public int getVersion() {
        return version;
    }
}
