package com.eldercare.backend.medication.internal.domain;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@DataGroup(DataGroup.Group.BUSINESS)
@Table(name = "family_medication_batch")
public class FamilyMedicationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "medication_id")
    private Long medicationId;

    @Column(name = "custom_name")
    private String customName;

    @Column(name = "custom_active_ingredient")
    private String customActiveIngredient;

    @Column(name = "custom_concentration", length = 100)
    private String customConcentration;

    @Column(name = "initial_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal initialQuantity;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "packaging_condition")
    private String packagingCondition;

    @Column(name = "has_prescription", nullable = false)
    private boolean hasPrescription;

    @Column(name = "file_ids", columnDefinition = "json")
    private String fileIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "deliverer_type", nullable = false)
    private DelivererType delivererType;

    @Column(name = "deliverer_id")
    private Long delivererId;

    @Column(name = "deliverer_name")
    private String delivererName;

    @Column(name = "relationship", length = 100)
    private String relationship;

    @Column(name = "purchase_request_id")
    private Long purchaseRequestId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FamilyMedicationBatchStatus status;

    @Column(name = "status_reason", length = 2000)
    private String statusReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Số đếm thực tế khi Hoàn trả, Hủy (Q-393). */
    @Column(name = "final_quantity", precision = 10, scale = 2)
    private BigDecimal finalQuantity;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by_id")
    private Long closedById;

    @Column(name = "return_recipient_name")
    private String returnRecipientName;

    @Column(name = "return_relationship", length = 100)
    private String returnRelationship;

    @Column(name = "witness")
    private String witness;

    public FamilyMedicationBatch() {}

    public FamilyMedicationBatch(
            Long residentId,
            Long medicationId,
            String customName,
            String customActiveIngredient,
            String customConcentration,
            BigDecimal initialQuantity,
            String unit,
            LocalDate expiryDate,
            String packagingCondition,
            boolean hasPrescription,
            String fileIds,
            DelivererType delivererType,
            Long delivererId,
            String delivererName,
            String relationship,
            Long purchaseRequestId,
            Long receiverId,
            Instant receivedAt,
            FamilyMedicationBatchStatus status,
            String statusReason,
            Instant now) {
        this.residentId = residentId;
        this.medicationId = medicationId;
        this.customName = customName;
        this.customActiveIngredient = customActiveIngredient;
        this.customConcentration = customConcentration;
        this.initialQuantity = initialQuantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
        this.packagingCondition = packagingCondition;
        this.hasPrescription = hasPrescription;
        this.fileIds = fileIds;
        this.delivererType = delivererType;
        this.delivererId = delivererId;
        this.delivererName = delivererName;
        this.relationship = relationship;
        this.purchaseRequestId = purchaseRequestId;
        this.receiverId = receiverId;
        this.receivedAt = receivedAt;
        this.status = status;
        this.statusReason = statusReason;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markInUse(Long medicationId, Instant now) {
        this.medicationId = medicationId;
        this.status = FamilyMedicationBatchStatus.IN_USE;
        this.statusReason = null;
        this.updatedAt = now;
    }

    public void markHoldOnly(String reason, Instant now) {
        this.status = FamilyMedicationBatchStatus.HOLD_ONLY;
        this.statusReason = reason;
        this.updatedAt = now;
    }

    /** Ghi dùng hết (Q-393): Điều dưỡng ghi, không do hệ thống tính. */
    public void markDepleted(Long actorId, Instant now) {
        this.status = FamilyMedicationBatchStatus.DEPLETED;
        close(actorId, now);
    }

    public void markReturned(
            String reason,
            BigDecimal countedQuantity,
            String recipientName,
            String relationship,
            Long actorId,
            Instant now) {
        this.status = FamilyMedicationBatchStatus.RETURNED;
        this.statusReason = reason;
        this.finalQuantity = countedQuantity;
        this.returnRecipientName = recipientName;
        this.returnRelationship = relationship;
        close(actorId, now);
    }

    public void markDisposed(String reason, BigDecimal countedQuantity, String witness, Long actorId, Instant now) {
        this.status = FamilyMedicationBatchStatus.DISPOSED;
        this.statusReason = reason;
        this.finalQuantity = countedQuantity;
        this.witness = witness;
        close(actorId, now);
    }

    private void close(Long actorId, Instant now) {
        this.closedById = actorId;
        this.closedAt = now;
        this.updatedAt = now;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Long getResidentId() {
        return residentId;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public String getCustomName() {
        return customName;
    }

    public String getCustomActiveIngredient() {
        return customActiveIngredient;
    }

    public String getCustomConcentration() {
        return customConcentration;
    }

    public BigDecimal getInitialQuantity() {
        return initialQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getPackagingCondition() {
        return packagingCondition;
    }

    public boolean isHasPrescription() {
        return hasPrescription;
    }

    public String getFileIds() {
        return fileIds;
    }

    public DelivererType getDelivererType() {
        return delivererType;
    }

    public Long getDelivererId() {
        return delivererId;
    }

    public String getDelivererName() {
        return delivererName;
    }

    public String getRelationship() {
        return relationship;
    }

    public Long getPurchaseRequestId() {
        return purchaseRequestId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public FamilyMedicationBatchStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public BigDecimal getFinalQuantity() {
        return finalQuantity;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Long getClosedById() {
        return closedById;
    }

    public String getReturnRecipientName() {
        return returnRecipientName;
    }

    public String getReturnRelationship() {
        return returnRelationship;
    }

    public String getWitness() {
        return witness;
    }
}
