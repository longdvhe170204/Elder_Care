package com.cambae.backend.m07.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Medication reconciliation response (FR-034 -> FR-038).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReconciliationResponse {

    private Long id;

    @JsonProperty("nctId")
    private Long residentId;

    @JsonProperty("lyDo")
    private String reason;

    @JsonProperty("lyDoLap")
    private String reasonDetail;

    @JsonProperty("trangThai")
    private String status;

    @JsonProperty("hanLuc")
    private Instant deadlineAt;

    @JsonProperty("quaHan")
    private Boolean overdue;

    @JsonProperty("nguoiLap")
    private String createdBy;

    @JsonProperty("nguoiGui")
    private String submittedBy;

    @JsonProperty("nguoiXacNhan")
    private String confirmedBy;

    @JsonProperty("lyDoTraLai")
    private String returnReason;

    @JsonProperty("dong")
    private List<ReconciliationItemResponse> items = new ArrayList<>();

    @JsonProperty("lenhDuocPhep")
    private List<String> permittedActions = new ArrayList<>();

    private int version;

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
}
