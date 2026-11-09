package com.eldercare.backend.medication.internal.handover.domain;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * Carried medication return record (FR-048a, FR-047). Dòng {@code kind = RETURN} của {@code medication_handover}; dòng
 * thuốc nhận lại trong {@code lines} (006 T069, {@code V006_97}).
 */
@Entity
@DataGroup(DataGroup.Group.MULTIPLE)
@Table(name = "medication_handover")
@SQLRestriction("kind = 'RETURN'")
@Immutable
public class MedicationReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kind", nullable = false, updatable = false, length = 20)
    private String kind = "RETURN";

    @Column(name = "resident_id", nullable = false, updatable = false)
    private Long residentId;

    @Column(name = "trip_id", updatable = false)
    private Long tripId;

    @Column(name = "handover_id", nullable = false, unique = true)
    private Long handoverId;

    @Column(name = "recorded_by_id", nullable = false)
    private Long recordedById;

    @Column(name = "return_time", nullable = false)
    private Instant returnTime;

    @Column(length = 2000)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Nhóm 3 trong bảng nhiều nhóm: khóa ngay khi thêm (data-model 006 mục 0.1). */
    @Column(name = "locked_at")
    private Instant lockedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "`lines`", columnDefinition = "json")
    private ReturnLines lines = new ReturnLines();

    protected MedicationReturn() {}

    /** Dòng nhận lại phải có đủ trước khi lưu: dòng bị khóa ngay khi thêm. */
    public MedicationReturn(
            MedicationHandover handover,
            Long recordedById,
            Instant returnTime,
            String reason,
            List<MedicationReturnItem> items,
            Instant now) {
        this.handoverId = handover.getId();
        this.residentId = handover.getResidentId();
        this.tripId = handover.getTripId();
        this.lines = new ReturnLines(items);
        this.lockedAt = now;
        this.recordedById = recordedById;
        this.returnTime = returnTime != null ? returnTime : now;
        this.reason = reason;
        this.createdAt = now;
    }

    public List<MedicationReturnItem> getItems() {
        return lines == null ? List.of() : lines.getItems();
    }

    public Long getResidentId() {
        return residentId;
    }

    public Instant getLockedAt() {
        return lockedAt;
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
