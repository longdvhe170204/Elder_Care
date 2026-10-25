package com.cambae.backend.m07.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AdjustInventoryBody {

    @NotNull(message = "Actual quantity is required")
    @DecimalMin(value = "0.0", message = "Actual quantity cannot be negative")
    private BigDecimal actualQuantity;

    @NotBlank(message = "Adjustment reason is required")
    private String reason;

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
