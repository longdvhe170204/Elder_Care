package com.cambae.backend.m07.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class DisposeBatchBody {

    @NotNull(message = "Disposal quantity is required")
    @DecimalMin(value = "0.01", message = "Disposal quantity must be > 0")
    private BigDecimal quantity;

    private Long representativeId;

    private String requestMethod;

    @NotBlank(message = "Disposal witness is required")
    private String witness;

    @NotBlank(message = "Disposal reason is required")
    private String reason;

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Long getRepresentativeId() {
        return representativeId;
    }

    public void setRepresentativeId(Long representativeId) {
        this.representativeId = representativeId;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getWitness() {
        return witness;
    }

    public void setWitness(String witness) {
        this.witness = witness;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
