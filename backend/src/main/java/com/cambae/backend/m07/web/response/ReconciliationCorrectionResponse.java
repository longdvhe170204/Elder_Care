package com.cambae.backend.m07.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * Medication reconciliation correction request response (FR-039).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReconciliationCorrectionResponse {

    private Long id;

    @JsonProperty("phieuDoiChieuId")
    private Long reconciliationId;

    @JsonProperty("noiDungDung")
    private Map<String, Object> correctedContent;

    @JsonProperty("lyDo")
    private String reason;

    @JsonProperty("trangThai")
    private String status;

    @JsonProperty("nguoiLapId")
    private Long createdById;

    @JsonProperty("thoiDiemLap")
    private Instant createdAt;

    @JsonProperty("nguoiXacNhanId")
    private Long confirmedById;

    @JsonProperty("thoiDiemXacNhan")
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
