package com.cambae.backend.m07.internal.service.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ReceiveBatchBody {

    private Instant receivedAt;

    private Long medicationId;

    private String customName;

    private String customActiveIngredient;

    private String customConcentration;

    @NotNull(message = "Initial quantity is required")
    @DecimalMin(value = "0.01", message = "Initial quantity must be > 0")
    private BigDecimal initialQuantity;

    @NotNull(message = "Unit is required")
    private String unit;

    private LocalDate expiryDate;

    private String packagingCondition;

    private boolean hasPrescription;

    private List<Long> fileIds;

    @NotNull(message = "Deliverer type is required")
    private String delivererType;

    private Long delivererId;

    private String delivererName;

    private String relationship;

    private Long purchaseRequestId;

    // Getters and Setters
    public Long getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getCustomActiveIngredient() {
        return customActiveIngredient;
    }

    public void setCustomActiveIngredient(String customActiveIngredient) {
        this.customActiveIngredient = customActiveIngredient;
    }

    public String getCustomConcentration() {
        return customConcentration;
    }

    public void setCustomConcentration(String customConcentration) {
        this.customConcentration = customConcentration;
    }

    public BigDecimal getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(BigDecimal initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getPackagingCondition() {
        return packagingCondition;
    }

    public void setPackagingCondition(String packagingCondition) {
        this.packagingCondition = packagingCondition;
    }

    public boolean isHasPrescription() {
        return hasPrescription;
    }

    public void setHasPrescription(boolean hasPrescription) {
        this.hasPrescription = hasPrescription;
    }

    public List<Long> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds) {
        this.fileIds = fileIds;
    }

    public String getDelivererType() {
        return delivererType;
    }

    public void setDelivererType(String delivererType) {
        this.delivererType = delivererType;
    }

    public Long getDelivererId() {
        return delivererId;
    }

    public void setDelivererId(Long delivererId) {
        this.delivererId = delivererId;
    }

    public String getDelivererName() {
        return delivererName;
    }

    public void setDelivererName(String delivererName) {
        this.delivererName = delivererName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public Long getPurchaseRequestId() {
        return purchaseRequestId;
    }

    public void setPurchaseRequestId(Long purchaseRequestId) {
        this.purchaseRequestId = purchaseRequestId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
