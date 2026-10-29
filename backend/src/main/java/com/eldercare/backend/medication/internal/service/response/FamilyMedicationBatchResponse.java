package com.eldercare.backend.medication.internal.service.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Lô thuốc gia đình gửi; không có số lượng còn, biến động (Q-393). */
public class FamilyMedicationBatchResponse {
    private String delivererType;
    private Long delivererId;
    private String delivererName;
    private String relationship;
    private Long purchaseRequestId;
    private Instant receivedAt;
    private List<Long> fileIds;
    private Long id;
    private Long residentId;
    private Long medicationId;
    private String customName;
    private String customActiveIngredient;
    private String customConcentration;
    private BigDecimal initialQuantity;
    private String unit;
    private LocalDate expiryDate;
    private String packagingCondition;
    private boolean hasPrescription;
    private String status;
    private String statusReason;
    private List<Long> boundPrescriptionIds;
    private BigDecimal finalQuantity;
    private Instant closedAt;

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

    public List<Long> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds) {
        this.fileIds = fileIds;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public List<Long> getBoundPrescriptionIds() {
        return boundPrescriptionIds;
    }

    public void setBoundPrescriptionIds(List<Long> boundPrescriptionIds) {
        this.boundPrescriptionIds = boundPrescriptionIds;
    }

    public BigDecimal getFinalQuantity() {
        return finalQuantity;
    }

    public void setFinalQuantity(BigDecimal finalQuantity) {
        this.finalQuantity = finalQuantity;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}
