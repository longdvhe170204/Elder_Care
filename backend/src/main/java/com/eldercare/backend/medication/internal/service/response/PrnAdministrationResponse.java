package com.eldercare.backend.medication.internal.service.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrnAdministrationResponse {

    private Long id;
    private Long prescriptionId;
    private Long residentId;
    private Instant administeredAt;
    private BigDecimal dosage;
    private String dosageUnit;
    private String reason;
    private Long administeredById;
    private String reportedBy;
    private boolean exceededLimit;
    private String adverseReaction;
    private String note;
    private boolean recordedAfterDisconnect;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }

    public Instant getAdministeredAt() {
        return administeredAt;
    }

    public void setAdministeredAt(Instant administeredAt) {
        this.administeredAt = administeredAt;
    }

    public BigDecimal getDosage() {
        return dosage;
    }

    public void setDosage(BigDecimal dosage) {
        this.dosage = dosage;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public void setDosageUnit(String dosageUnit) {
        this.dosageUnit = dosageUnit;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getAdministeredById() {
        return administeredById;
    }

    public void setAdministeredById(Long administeredById) {
        this.administeredById = administeredById;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public boolean isExceededLimit() {
        return exceededLimit;
    }

    public void setExceededLimit(boolean exceededLimit) {
        this.exceededLimit = exceededLimit;
    }

    public String getAdverseReaction() {
        return adverseReaction;
    }

    public void setAdverseReaction(String adverseReaction) {
        this.adverseReaction = adverseReaction;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isRecordedAfterDisconnect() {
        return recordedAfterDisconnect;
    }

    public void setRecordedAfterDisconnect(boolean recordedAfterDisconnect) {
        this.recordedAfterDisconnect = recordedAfterDisconnect;
    }
}
