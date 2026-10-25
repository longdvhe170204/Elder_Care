package com.cambae.backend.m07.internal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "family_medication_movement")
public class FamilyMedicationMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "remaining_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingAfter;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "moved_at", nullable = false)
    private Instant movedAt;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Column(name = "witness")
    private String witness;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "relationship", length = 100)
    private String relationship;

    public FamilyMedicationMovement() {}

    public FamilyMedicationMovement(
            Long batchId,
            MovementType movementType,
            BigDecimal quantity,
            BigDecimal remainingAfter,
            String sourceType,
            Long sourceId,
            Long actorId,
            Instant movedAt,
            String reason,
            String witness,
            String recipientName,
            String relationship) {
        this.batchId = batchId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.remainingAfter = remainingAfter;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.actorId = actorId;
        this.movedAt = movedAt;
        this.reason = reason;
        this.witness = witness;
        this.recipientName = recipientName;
        this.relationship = relationship;
    }

    public Long getId() {
        return id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getRemainingAfter() {
        return remainingAfter;
    }

    public String getSourceType() {
        return sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public Long getActorId() {
        return actorId;
    }

    public Instant getMovedAt() {
        return movedAt;
    }

    public String getReason() {
        return reason;
    }

    public String getWitness() {
        return witness;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRelationship() {
        return relationship;
    }
}
