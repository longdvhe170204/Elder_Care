package com.eldercare.backend.medication.internal.reconciliation.domain;

import java.time.Instant;

/**
 * Đề nghị đính chính phiếu đối chiếu đã xác nhận (BR-M07-17): dòng {@code simple_record} loại
 * {@code M07.RECONCILIATION_CORRECTION_REQUEST} (006 T069, {@code V006_98}).
 */
public class ReconciliationCorrectionRequest {

    private Long id;

    private Long reconciliationId;

    private String correctedContentJson;

    private String reason;

    private Long createdById;

    private Instant createdAt;

    private ReconciliationCorrectionStatus status = ReconciliationCorrectionStatus.PENDING_CONFIRMATION;

    private Long confirmedById;

    private Instant confirmedAt;

    private Long correctionId;

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

    private int version;

    /** Gán id, phiên bản của dòng bảng chung sau khi đọc hoặc ghi. */
    public void restoreRow(long id, int version) {
        this.id = id;
        this.version = version;
    }

    public static ReconciliationCorrectionRequest restore(
            ReconciliationCorrectionStatus status, Instant createdAt, ReconciliationCorrectionRequestContent c) {
        ReconciliationCorrectionRequest r = new ReconciliationCorrectionRequest(
                c.reconciliationId(), c.correctedContentJson(), c.reason(), c.createdById(), createdAt, createdAt);
        r.status = status;
        r.confirmedById = c.confirmedById();
        r.confirmedAt = c.confirmedAt();
        r.correctionId = c.correctionId();
        r.returnReason = c.returnReason();
        return r;
    }

    public ReconciliationCorrectionRequestContent content() {
        return new ReconciliationCorrectionRequestContent(
                reconciliationId,
                correctedContentJson,
                reason,
                createdById,
                confirmedById,
                confirmedAt,
                correctionId,
                returnReason,
                null);
    }

    public int getVersion() {
        return version;
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
