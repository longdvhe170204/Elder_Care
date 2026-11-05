package com.eldercare.backend.medication.internal.handover.domain;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * Carried medication handover entity (FR-048a). Dòng {@code kind = HANDOVER} của {@code medication_handover}; dòng
 * thuốc giao, liều mang theo trong {@code lines} (006 T069, {@code V006_97}).
 */
@Entity
@DataGroup(DataGroup.Group.MULTIPLE)
@Table(name = "medication_handover")
@SQLRestriction("kind = 'HANDOVER'")
public class MedicationHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "absence_interval_id")
    private Long absenceIntervalId;

    @Column(name = "trip_id")
    private Long tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 30)
    private RecipientType recipientType;

    @Column(name = "recipient_id")
    private Long recipientId;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(length = 100)
    private String relationship;

    @Column(name = "dispensed_by_id", nullable = false)
    private Long dispensedById;

    @Column(name = "handover_time", nullable = false)
    private Instant handoverTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HandoverStatus status = HandoverStatus.HANDED_OVER;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "`lines`", columnDefinition = "json")
    private HandoverLines lines = new HandoverLines();

    protected MedicationHandover() {}

    public MedicationHandover(
            Long residentId,
            Long absenceIntervalId,
            Long tripId,
            RecipientType recipientType,
            Long recipientId,
            String recipientName,
            String relationship,
            Long dispensedById,
            Instant handoverTime,
            Instant now) {
        this.residentId = residentId;
        this.absenceIntervalId = absenceIntervalId;
        this.tripId = tripId;
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.relationship = relationship;
        this.dispensedById = dispensedById;
        this.handoverTime = handoverTime != null ? handoverTime : now;
        this.status = HandoverStatus.HANDED_OVER;
        this.createdAt = now;
    }

    public List<MedicationHandoverItem> getItems() {
        return lines == null ? List.of() : lines.getItems();
    }

    public List<MedicationHandoverDose> getDoses() {
        return lines == null ? List.of() : lines.getDoses();
    }

    /** Thay toàn bộ dòng; lần giao đã nhận lại bị khóa ở cơ sở dữ liệu. */
    public void replaceLines(List<MedicationHandoverItem> items, List<MedicationHandoverDose> doses) {
        this.lines = new HandoverLines(items, doses);
    }

    public void markWaitingReturn() {
        this.status = HandoverStatus.WAITING_RETURN;
    }

    public void markReturned(Instant now) {
        this.status = HandoverStatus.RETURNED;
        this.lockedAt = now;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public Long getResidentId() {
        return residentId;
    }

    public Long getAbsenceIntervalId() {
        return absenceIntervalId;
    }

    public Long getTripId() {
        return tripId;
    }

    public RecipientType getRecipientType() {
        return recipientType;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRelationship() {
        return relationship;
    }

    public Long getDispensedById() {
        return dispensedById;
    }

    public Instant getHandoverTime() {
        return handoverTime;
    }

    public HandoverStatus getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }
}
