package com.eldercare.backend.medication.internal.domain;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * No-medication order for trip (FR-048b).
 */
@Entity
@DataGroup(DataGroup.Group.RECORD)
@Table(name = "no_medication_order")
public class NoMedicationOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NoMedicationOrder() {}

    public NoMedicationOrder(Long residentId, Long tripId, String reason, Long createdById, Instant now) {
        this.residentId = residentId;
        this.tripId = tripId;
        this.reason = reason;
        this.createdById = createdById;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getResidentId() {
        return residentId;
    }

    public Long getTripId() {
        return tripId;
    }

    public String getReason() {
        return reason;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
