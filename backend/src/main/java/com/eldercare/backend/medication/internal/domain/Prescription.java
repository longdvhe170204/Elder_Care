package com.eldercare.backend.medication.internal.domain;

import com.eldercare.backend.common.data.DataGroup;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * Prescription Entity (FR-004 -> FR-017, data-model section 4).
 * Group 2 - State-managed business entity. Table: prescription.
 */
@Entity
@Table(name = "prescription")
@DataGroup(DataGroup.Group.BUSINESS)
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "medication_id", nullable = false)
    private Long medicationId;

    @Column(name = "dosage", nullable = false, precision = 10, scale = 2)
    private BigDecimal dosage;

    @Column(name = "dosage_unit", nullable = false, length = 50)
    private String dosageUnit;

    @Column(name = "route", nullable = false, length = 50)
    private String route;

    @Enumerated(EnumType.STRING)
    @Column(name = "prescription_type", nullable = false)
    private PrescriptionType prescriptionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_type")
    private FrequencyType frequencyType;

    @Column(name = "timed_slots", columnDefinition = "json")
    private String timedSlots;

    @JdbcTypeCode(Types.SMALLINT)
    @Column(name = "every_n_hours")
    private Integer everyNHours;

    @Column(name = "first_slot")
    private Instant firstSlot;

    @Column(name = "days_of_week")
    private String daysOfWeek;

    @Column(name = "min_interval_minutes")
    private Integer minIntervalMinutes;

    @JdbcTypeCode(Types.SMALLINT)
    @Column(name = "max_doses_per_24h")
    private Integer maxDosesPer24h;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private LocalDate endedAt;

    @Column(name = "suspended_from")
    private Instant suspendedFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "medication_source", nullable = false)
    private MedicationSource medicationSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "prescriber_type", nullable = false)
    private PrescriberType prescriberType;

    @Column(name = "facility_name")
    private String facilityName;

    @Column(name = "prescriber_name")
    private String prescriberName;

    @Column(name = "prescriber_id")
    private Long prescriberId;

    @Column(name = "prescribed_at")
    private LocalDate prescribedAt;

    @Column(name = "prescription_file_id")
    private Long prescriptionFileId;

    @Column(name = "instruction_note", length = 2000)
    private String instructionNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PrescriptionStatus status = PrescriptionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspension_reason")
    private SuspensionReason suspensionReason;

    @Column(name = "pending_admission", nullable = false)
    private boolean pendingAdmission;

    @Column(name = "replaces_id", unique = true)
    private Long replacesId;

    @Enumerated(EnumType.STRING)
    @Column(name = "termination_reason")
    private TerminationReason terminationReason;

    @Column(name = "reconciliation_ticket_id")
    private Long reconciliationTicketId;

    @Column(name = "expiration_warned_at")
    private Instant expirationWarnedAt;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "prescription_active_ingredient", joinColumns = @JoinColumn(name = "prescription_id"))
    @Column(name = "allergen_id", nullable = false)
    private Set<Long> allergenIds = new HashSet<>();

    protected Prescription() {}

    public Prescription(
            Long residentId,
            Long medicationId,
            BigDecimal dosage,
            String dosageUnit,
            String route,
            PrescriptionType prescriptionType,
            FrequencyType frequencyType,
            String timedSlots,
            Integer everyNHours,
            Instant firstSlot,
            String daysOfWeek,
            Integer minIntervalMinutes,
            Integer maxDosesPer24h,
            Instant startedAt,
            LocalDate endedAt,
            MedicationSource medicationSource,
            PrescriberType prescriberType,
            String facilityName,
            String prescriberName,
            Long prescriberId,
            LocalDate prescribedAt,
            Long prescriptionFileId,
            String instructionNote,
            PrescriptionStatus status,
            boolean pendingAdmission,
            Long replacesId,
            Long reconciliationTicketId,
            Long creatorId,
            Instant createdAt,
            Set<Long> allergenIds,
            Instant now) {
        this.residentId = residentId;
        this.medicationId = medicationId;
        this.dosage = dosage;
        this.dosageUnit = dosageUnit;
        this.route = route;
        this.prescriptionType = prescriptionType;
        this.frequencyType = frequencyType;
        this.timedSlots = timedSlots;
        this.everyNHours = everyNHours;
        this.firstSlot = firstSlot;
        this.daysOfWeek = daysOfWeek;
        this.minIntervalMinutes = minIntervalMinutes;
        this.maxDosesPer24h = maxDosesPer24h;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.medicationSource = medicationSource;
        this.prescriberType = prescriberType;
        this.facilityName = facilityName;
        this.prescriberName = prescriberName;
        this.prescriberId = prescriberId;
        this.prescribedAt = prescribedAt;
        this.prescriptionFileId = prescriptionFileId;
        this.instructionNote = instructionNote;
        this.status = status != null ? status : PrescriptionStatus.ACTIVE;
        this.pendingAdmission = pendingAdmission;
        this.replacesId = replacesId;
        this.reconciliationTicketId = reconciliationTicketId;
        this.creatorId = creatorId;
        this.createdAt = createdAt != null ? createdAt : now;
        if (allergenIds != null) {
            this.allergenIds.addAll(allergenIds);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getResidentId() {
        return residentId;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public BigDecimal getDosage() {
        return dosage;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public String getRoute() {
        return route;
    }

    public PrescriptionType getPrescriptionType() {
        return prescriptionType;
    }

    public FrequencyType getFrequencyType() {
        return frequencyType;
    }

    public String getTimedSlots() {
        return timedSlots;
    }

    public Integer getEveryNHours() {
        return everyNHours;
    }

    public Instant getFirstSlot() {
        return firstSlot;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public Integer getMinIntervalMinutes() {
        return minIntervalMinutes;
    }

    public Integer getMaxDosesPer24h() {
        return maxDosesPer24h;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public LocalDate getEndedAt() {
        return endedAt;
    }

    public void changeEndDate(LocalDate endedAt) {
        this.endedAt = endedAt;
    }

    public Instant getSuspendedFrom() {
        return suspendedFrom;
    }

    public void scheduleSuspensionFrom(Instant suspendedFrom) {
        this.suspendedFrom = suspendedFrom;
    }

    public void cancelScheduledSuspension() {
        this.suspendedFrom = null;
    }

    public MedicationSource getMedicationSource() {
        return medicationSource;
    }

    public PrescriberType getPrescriberType() {
        return prescriberType;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public String getPrescriberName() {
        return prescriberName;
    }

    public Long getPrescriberId() {
        return prescriberId;
    }

    public LocalDate getPrescribedAt() {
        return prescribedAt;
    }

    public Long getPrescriptionFileId() {
        return prescriptionFileId;
    }

    public String getInstructionNote() {
        return instructionNote;
    }

    public void changeInstructionNote(String instructionNote) {
        this.instructionNote = instructionNote;
    }

    public PrescriptionStatus getStatus() {
        return status;
    }

    public SuspensionReason getSuspensionReason() {
        return suspensionReason;
    }

    public boolean isPendingAdmission() {
        return pendingAdmission;
    }

    public Long getReplacesId() {
        return replacesId;
    }

    public TerminationReason getTerminationReason() {
        return terminationReason;
    }

    public Long getReconciliationTicketId() {
        return reconciliationTicketId;
    }

    public Instant getExpirationWarnedAt() {
        return expirationWarnedAt;
    }

    public void markExpirationWarned(Instant warnedAt) {
        this.expirationWarnedAt = warnedAt;
    }

    public void clearExpirationWarning() {
        this.expirationWarnedAt = null;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getVersion() {
        return version;
    }

    public Set<Long> getAllergenIds() {
        return Collections.unmodifiableSet(allergenIds);
    }

    public void markReplaces(Long replacesId) {
        this.replacesId = replacesId;
    }

    public void suspend(SuspensionReason reason) {
        this.status = PrescriptionStatus.SUSPENDED;
        this.suspensionReason = reason;
    }

    public void resume() {
        this.status = PrescriptionStatus.ACTIVE;
        this.suspensionReason = null;
    }

    public void activate() {
        this.status = PrescriptionStatus.ACTIVE;
    }

    public void discontinue() {
        this.discontinue(TerminationReason.DISCONTINUED);
    }

    public void discontinue(TerminationReason reason) {
        this.status = PrescriptionStatus.DISCONTINUED;
        this.terminationReason = reason != null ? reason : TerminationReason.DISCONTINUED;
    }

    public void expire() {
        this.status = PrescriptionStatus.EXPIRED;
        this.terminationReason = TerminationReason.EXPIRED;
    }
}
