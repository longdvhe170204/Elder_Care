package com.cambae.backend.m07.internal.domain;

import com.cambae.backend.common.data.DataGroup;
import com.cambae.backend.m07.api.dto.MedicationSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Medication Dose Entity (Group 2 - Business with lifecycle; FR-018 -> FR-023; data-model section 5).
 * Table {@code medication_dose}. Only materialized doses have rows in this table.
 */
@Entity
@Table(name = "medication_dose")
@DataGroup(DataGroup.Group.BUSINESS)
public class MedicationDose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "medication_id", nullable = false)
    private Long medicationId;

    @Column(name = "scheduled_time", nullable = false)
    private Instant scheduledTime;

    @Column(name = "window_from", nullable = false)
    private Instant windowFrom;

    @Column(name = "window_to", nullable = false)
    private Instant windowTo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dosage;

    @Column(name = "dosage_unit", nullable = false, length = 50)
    private String dosageUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "medication_source", nullable = false, length = 30)
    private MedicationSource medicationSource;

    @Column(name = "special_control", nullable = false)
    private boolean specialControl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DoseStatus status = DoseStatus.UPCOMING;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "late_at")
    private Instant lateAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 50)
    private DoseCancellationReason cancellationReason;

    @Column(name = "closed_by_type", length = 30)
    private String closedByType;

    @Column(name = "closed_by_id")
    private Long closedById;

    @Column(name = "closure_rule", length = 50)
    private String closureRule;

    @Column(name = "pending_post_return_recording", nullable = false)
    private boolean pendingPostReturnRecording = false;

    @Column(name = "late_reminded_at")
    private Instant lateRemindedAt;

    @Column(name = "floor_area_id")
    private Long floorAreaId;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MedicationDose() {}

    public MedicationDose(
            Long prescriptionId,
            Long residentId,
            Long medicationId,
            Instant scheduledTime,
            Instant windowFrom,
            Instant windowTo,
            BigDecimal dosage,
            String dosageUnit,
            MedicationSource medicationSource,
            boolean specialControl,
            DoseStatus status,
            Long floorAreaId,
            Instant now) {
        this.prescriptionId = prescriptionId;
        this.residentId = residentId;
        this.medicationId = medicationId;
        this.scheduledTime = scheduledTime;
        this.windowFrom = windowFrom;
        this.windowTo = windowTo;
        this.dosage = dosage;
        this.dosageUnit = dosageUnit;
        this.medicationSource = medicationSource;
        this.specialControl = specialControl;
        this.status = status != null ? status : DoseStatus.UPCOMING;
        this.floorAreaId = floorAreaId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markDue(Instant now) {
        DoseTransitionTable.validateTransition(this.status, DoseStatus.DUE);
        this.status = DoseStatus.DUE;
        this.dueAt = now;
        this.updatedAt = now;
    }

    public void markLate(Instant now) {
        DoseTransitionTable.validateTransition(this.status, DoseStatus.LATE);
        this.status = DoseStatus.LATE;
        this.lateAt = now;
        this.updatedAt = now;
    }

    public void markLateReminded(Instant now) {
        this.lateRemindedAt = now;
        this.updatedAt = now;
    }

    public void closeAdministered(Long closerId, String closerType, Long floorId, Instant now) {
        DoseTransitionTable.validateTransition(this.status, DoseStatus.ADMINISTERED);
        this.status = DoseStatus.ADMINISTERED;
        this.closedAt = now;
        this.closedById = closerId;
        this.closedByType = closerType;
        if (floorId != null) {
            this.floorAreaId = floorId;
        }
        this.updatedAt = now;
    }

    public void closeRefused(Long closerId, String closerType, Long floorId, Instant now) {
        DoseTransitionTable.validateTransition(this.status, DoseStatus.REFUSED);
        this.status = DoseStatus.REFUSED;
        this.closedAt = now;
        this.closedById = closerId;
        this.closedByType = closerType;
        if (floorId != null) {
            this.floorAreaId = floorId;
        }
        this.updatedAt = now;
    }

    public void closeNotAdministered(Long closerId, String closerType, Long floorId, Instant now) {
        DoseTransitionTable.validateTransition(this.status, DoseStatus.NOT_ADMINISTERED);
        this.status = DoseStatus.NOT_ADMINISTERED;
        this.closedAt = now;
        this.closedById = closerId;
        this.closedByType = closerType;
        if (floorId != null) {
            this.floorAreaId = floorId;
        }
        this.updatedAt = now;
    }

    public void closeMissedBySystem(Instant now) {
        DoseTransitionTable.validateTransition(this.status, DoseStatus.MISSED);
        this.status = DoseStatus.MISSED;
        this.closedAt = now;
        this.closedByType = "SYSTEM";
        this.closureRule = "BR_M07_02";
        this.updatedAt = now;
    }

    public void cancel(DoseCancellationReason reason, Instant now) {
        DoseTransitionTable.validateTransition(this.status, DoseStatus.CANCELLED);
        this.status = DoseStatus.CANCELLED;
        this.cancellationReason = reason;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void suspend(Instant now) {
        DoseTransitionTable.validateTransition(this.status, DoseStatus.SUSPENDED);
        this.status = DoseStatus.SUSPENDED;
        this.updatedAt = now;
    }

    public void expireSuspension(Instant now) {
        this.endedAt = now;
        this.updatedAt = now;
    }

    public void markLeaveMedication(Instant now) {
        DoseTransitionTable.validateTransition(this.status, DoseStatus.LEAVE_MEDICATION);
        this.status = DoseStatus.LEAVE_MEDICATION;
        this.updatedAt = now;
    }

    public void resumeFromSuspension(Instant now) {
        DoseStatus target = now.isBefore(this.windowFrom) ? DoseStatus.UPCOMING : DoseStatus.DUE;
        DoseTransitionTable.validateTransition(this.status, target);
        this.status = target;
        if (target == DoseStatus.DUE) {
            this.dueAt = now;
        }
        this.updatedAt = now;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public Long getResidentId() {
        return residentId;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public Instant getScheduledTime() {
        return scheduledTime;
    }

    public Instant getWindowFrom() {
        return windowFrom;
    }

    public Instant getWindowTo() {
        return windowTo;
    }

    public BigDecimal getDosage() {
        return dosage;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public MedicationSource getMedicationSource() {
        return medicationSource;
    }

    public boolean isSpecialControl() {
        return specialControl;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getLateAt() {
        return lateAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public DoseCancellationReason getCancellationReason() {
        return cancellationReason;
    }

    public String getClosedByType() {
        return closedByType;
    }

    public Long getClosedById() {
        return closedById;
    }

    public String getClosureRule() {
        return closureRule;
    }

    public boolean isPendingPostReturnRecording() {
        return pendingPostReturnRecording;
    }

    public Instant getLateRemindedAt() {
        return lateRemindedAt;
    }

    public Long getFloorAreaId() {
        return floorAreaId;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
