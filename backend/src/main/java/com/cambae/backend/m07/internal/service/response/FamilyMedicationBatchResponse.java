package com.cambae.backend.m07.internal.service.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
    private BigDecimal remainingQuantity;
    private String unit;
    private LocalDate expiryDate;
    private String packagingCondition;
    private boolean hasPrescription;
    private String status;
    private String statusReason;
    private Integer daysRemaining;
    private List<Long> boundPrescriptionIds;
    private List<MovementResponse> movements;

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

    public static class MovementResponse {
        private Long id;
        private String movementType;
        private BigDecimal quantity;
        private BigDecimal remainingAfter;
        private Instant movedAt;
        private String reason;
        private Long actorId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getMovementType() {
            return movementType;
        }

        public void setMovementType(String movementType) {
            this.movementType = movementType;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getRemainingAfter() {
            return remainingAfter;
        }

        public void setRemainingAfter(BigDecimal remainingAfter) {
            this.remainingAfter = remainingAfter;
        }

        public Instant getMovedAt() {
            return movedAt;
        }

        public void setMovedAt(Instant movedAt) {
            this.movedAt = movedAt;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public Long getActorId() {
            return actorId;
        }

        public void setActorId(Long actorId) {
            this.actorId = actorId;
        }
    }

    // Getters and Setters
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

    public BigDecimal getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(BigDecimal remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
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

    public Integer getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Integer daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public List<Long> getBoundPrescriptionIds() {
        return boundPrescriptionIds;
    }

    public void setBoundPrescriptionIds(List<Long> boundPrescriptionIds) {
        this.boundPrescriptionIds = boundPrescriptionIds;
    }

    public List<MovementResponse> getMovements() {
        return movements;
    }

    public void setMovements(List<MovementResponse> movements) {
        this.movements = movements;
    }
}
