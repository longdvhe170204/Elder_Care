package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ReturnItemBody {

    @NotNull
    @JsonAlias({"donThuocId", "prescriptionId"})
    private Long prescriptionId;

    @NotNull
    @DecimalMin(value = "0.0", message = "Quantity cannot be negative")
    @JsonAlias({"soLuong", "quantity"})
    private BigDecimal quantity;

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
