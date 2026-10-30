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
 * Medication reconciliation correction request entity (FR-039, BR-M07-17).
 */
@Entity
@DataGroup(DataGroup.Group.BUSINESS)
@Table(name = "reconciliation_correction_request")
public class ReconciliationCorrectionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reconciliation_id", nullable = false)
    private Long reconciliationId;

    @Column(name = "corrected_content_json", columnDefinition = "json", nullable = false)
    private String correctedContentJson;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReconciliationCorrectionStatus status = ReconciliationCorrectionStatus.PENDING_CONFIRMATION;

    @Column(name = "confirmed_by_id")
    private Long confirmedById;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "correction_id")
    private Long correctionId;

    @Column(name = "return_reason", length = 2000)
    private String returnReason;

    protected ReconciliationCorrectionRequest() {}

    public ReconciliationCorrectionRequest(
            Long reconciliationId,
            String correctedContentJson,
            String reason,
            Long createdById,
            Instant createdAt,
            Instant now) {
        this.reconciliationId = reconciliationId;
        this.correctedContentJson = correctedContentJson;
        this.reason = reason;
        this.createdById = createdById;
        this.createdAt = createdAt != null ? createdAt : now;
        this.status = ReconciliationCorrectionStatus.PENDING_CONFIRMATION;
    }

    public void confirm(Long confirmedById, Instant confirmedAt, Long correctionId) {
        this.status = ReconciliationCorrectionStatus.CONFIRMED;
        this.confirmedById = confirmedById;
        this.confirmedAt = confirmedAt;
        this.correctionId = correctionId;
    }

    public void reject(Long rejectedById, Instant rejectedAt, String returnReason) {
        this.returnReason = returnReason;
        this.status = ReconciliationCorrectionStatus.REJECTED;
        this.confirmedById = rejectedById;
        this.confirmedAt = rejectedAt;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public Long getReconciliationId() {
        return reconciliationId;
    }

    public String getCorrectedContentJson() {
        return correctedContentJson;
    }

    public String getReason() {
        return reason;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ReconciliationCorrectionStatus getStatus() {
        return status;
    }

    public Long getConfirmedById() {
        return confirmedById;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Long getCorrectionId() {
        return correctionId;
    }
}
