package com.cambae.backend.m07.internal.service.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public class ReturnBatchBody {

    @NotNull(message = "Return quantity is required")
    @DecimalMin(value = "0.01", message = "Return quantity must be > 0")
    private BigDecimal quantity;

    @NotBlank(message = "Return recipient name is required")
    private String recipientName;

    private String relationship;

    private Instant returnedAt;

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }
}
