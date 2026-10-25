package com.cambae.backend.m07.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RecordIntakeBody {

    @NotNull(message = "Procurement request line is required")
    private Long procurementLineId;

    @NotNull(message = "Intake result is required (PASS/FAIL)")
    private String intakeResult;

    @NotNull(message = "Received quantity is required")
    @DecimalMin(value = "0.01", message = "Received quantity must be > 0")
    private BigDecimal quantityReceived;

    private String discrepancy;

    private String reason;

    private String note;

    public Long getProcurementLineId() {
        return procurementLineId;
    }

    public void setProcurementLineId(Long procurementLineId) {
        this.procurementLineId = procurementLineId;
    }

    public String getIntakeResult() {
        return intakeResult;
    }

    public void setIntakeResult(String intakeResult) {
        this.intakeResult = intakeResult;
    }

    public BigDecimal getQuantityReceived() {
        return quantityReceived;
    }

    public void setQuantityReceived(BigDecimal quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    public String getDiscrepancy() {
        return discrepancy;
    }

    public void setDiscrepancy(String discrepancy) {
        this.discrepancy = discrepancy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
