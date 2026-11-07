package com.eldercare.backend.medication.internal.handover.domain;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;

/**
 * No-medication order for trip (FR-048b). Dòng {@code kind = NO_MEDICATION} của {@code medication_handover}; một lệnh
 * mỗi (người, chuyến) bằng cột sinh {@code no_medication_key} (006 T069, {@code V006_97}).
 */
@Entity
@DataGroup(DataGroup.Group.MULTIPLE)
@Table(name = "medication_handover")
@SQLRestriction("kind = 'NO_MEDICATION'")
@Immutable
public class NoMedicationOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kind", nullable = false, updatable = false, length = 20)
    private String kind = "NO_MEDICATION";

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

    /** Nhóm 3 trong bảng nhiều nhóm: khóa ngay khi thêm (data-model 006 mục 0.1). */
    @Column(name = "locked_at")
    private Instant lockedAt;

    protected NoMedicationOrder() {}

    public NoMedicationOrder(Long residentId, Long tripId, String reason, Long createdById, Instant now) {
        this.residentId = residentId;
        this.tripId = tripId;
        this.reason = reason;
        this.createdById = createdById;
        this.createdAt = now;
        this.lockedAt = now;
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

    public Instant getLockedAt() {
        return lockedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
