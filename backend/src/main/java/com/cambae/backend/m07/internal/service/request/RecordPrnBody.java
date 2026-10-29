package com.cambae.backend.m07.internal.service.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for recording a PRN dose administration (LanDungPrnMoi - FR-024, FR-025, FR-027).
 */
public class RecordPrnBody {

    @NotNull
    @DecimalMin(value = "0.001", message = "Dosage must be greater than 0")
    private BigDecimal dosage;

    private String dosageUnit;

    @NotEmpty(message = "PRN medication reason is required (FR-024)")
    private String reason;

    private String reportedBy;

    private Instant offlineDeviceTime;

    private Instant administeredAt;

    private String adverseReaction;

    private String note;

    private boolean recordedAfterDisconnect;

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

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public Instant getOfflineDeviceTime() {
        return offlineDeviceTime;
    }

    public void setOfflineDeviceTime(Instant offlineDeviceTime) {
        this.offlineDeviceTime = offlineDeviceTime;
    }

    public boolean isReported() {
        return reportedBy != null && !reportedBy.isBlank();
    }

    public Instant getAdministeredAt() {
        return administeredAt;
    }

    public void setAdministeredAt(Instant administeredAt) {
        this.administeredAt = administeredAt;
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
