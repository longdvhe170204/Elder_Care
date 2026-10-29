package com.eldercare.backend.medication.internal.service.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Medication reconciliation correction request response (FR-039).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReconciliationCorrectionResponse {

    private Long id;
    private Long reconciliationId;
    private Map<String, Object> correctedContent;
    private String reason;
    private String status;
    private Long createdById;
    private Instant createdAt;
    private Long confirmedById;
    private Instant confirmedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReconciliationId() {
        return reconciliationId;
    }

    public void setReconciliationId(Long reconciliationId) {
        this.reconciliationId = reconciliationId;
    }

    public Map<String, Object> getCorrectedContent() {
        return correctedContent;
    }

    public void setCorrectedContent(Map<String, Object> correctedContent) {
        this.correctedContent = correctedContent;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getConfirmedById() {
        return confirmedById;
    }

    public void setConfirmedById(Long confirmedById) {
        this.confirmedById = confirmedById;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}
