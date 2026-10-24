package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public class ReturnBatchBody {

    @NotNull(message = "Return quantity is required")
    @DecimalMin(value = "0.01", message = "Return quantity must be > 0")
    @JsonAlias({"soLuong", "quantity"})
    private BigDecimal quantity;

    @NotBlank(message = "Return recipient name is required")
    @JsonAlias({"nguoiNhanTen", "recipientName"})
    private String recipientName;

    @JsonAlias({"quanHe", "relationship"})
    private String relationship;

    @JsonAlias({"thoiDiem", "returnedAt"})
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
