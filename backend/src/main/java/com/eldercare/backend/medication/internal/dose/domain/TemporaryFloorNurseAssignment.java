package com.eldercare.backend.medication.internal.dose.domain;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Điều dưỡng tầng khác được Quản lý viện chỉ định phụ trách tạm khi tầng không có Điều dưỡng nào có ca (BR-M07-31, Q-436).
 * Bảng {@code temporary_floor_nurse_assignment}; quyền hết ở {@code expiresAt}.
 */
@Entity
@Table(name = "temporary_floor_nurse_assignment")
@DataGroup(DataGroup.Group.BUSINESS)
public class TemporaryFloorNurseAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "floor_area_id", nullable = false)
    private Long floorAreaId;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "nurse_account_id", nullable = false)
    private Long nurseAccountId;

    @Column(name = "assigned_by_id", nullable = false)
    private Long assignedById;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected TemporaryFloorNurseAssignment() {}

    public TemporaryFloorNurseAssignment(
            Long floorAreaId,
            Long shiftId,
            Long nurseAccountId,
            Long assignedById,
            Instant assignedAt,
            Instant expiresAt) {
        this.floorAreaId = floorAreaId;
        this.shiftId = shiftId;
        this.nurseAccountId = nurseAccountId;
        this.assignedById = assignedById;
        this.assignedAt = assignedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public Long getFloorAreaId() {
        return floorAreaId;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public Long getNurseAccountId() {
        return nurseAccountId;
    }

    public Long getAssignedById() {
        return assignedById;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
