package com.cambae.backend.m07.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Carried medication return record (FR-048a, FR-047).
 */
@Entity
@Table(name = "medication_return")
public class MedicationReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "handover_id", nullable = false, unique = true)
    private Long handoverId;

    @Column(name = "recorded_by_id", nullable = false)
    private Long recordedById;

    @Column(name = "return_time", nullable = false)
    private Instant returnTime;

    @Column(length = 2000)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MedicationReturn() {}

    public MedicationReturn(Long handoverId, Long recordedById, Instant returnTime, String reason) {
        this.handoverId = handoverId;
        this.recordedById = recordedById;
        this.returnTime = returnTime != null ? returnTime : Instant.now();
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getHandoverId() {
        return handoverId;
    }

    public Long getRecordedById() {
        return recordedById;
    }

    public Instant getReturnTime() {
        return returnTime;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
