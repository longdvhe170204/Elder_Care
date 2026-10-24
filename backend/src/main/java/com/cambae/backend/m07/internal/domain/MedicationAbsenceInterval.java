package com.cambae.backend.m07.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Absence interval affecting medication schedule (FR-048, BR-M07-03).
 */
@Entity
@Table(name = "medication_absence_interval")
public class MedicationAbsenceInterval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "absence_type", nullable = false, length = 35)
    private AbsenceIntervalType absenceType;

    @Column(name = "from_time", nullable = false)
    private Instant fromTime;

    @Column(name = "expected_to_time")
    private Instant expectedToTime;

    @Column(name = "actual_to_time")
    private Instant actualToTime;

    @Column(name = "absence_session_id")
    private Long absenceSessionId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "no_medication_order_id")
    private Long noMedicationOrderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MedicationAbsenceInterval() {}

    public MedicationAbsenceInterval(
            Long residentId,
            AbsenceIntervalType absenceType,
            Instant fromTime,
            Instant expectedToTime,
            Long absenceSessionId,
            Long tripId) {
        this.residentId = residentId;
        this.absenceType = absenceType;
        this.fromTime = fromTime;
        this.expectedToTime = expectedToTime;
        this.absenceSessionId = absenceSessionId;
        this.tripId = tripId;
        this.createdAt = Instant.now();
    }

    public void close(Instant actualToTime) {
        this.actualToTime = actualToTime != null ? actualToTime : Instant.now();
    }

    public void extend(Instant newExpectedToTime) {
        this.expectedToTime = newExpectedToTime;
    }

    public boolean isOpen() {
        return this.actualToTime == null;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public Long getResidentId() {
        return residentId;
    }

    public AbsenceIntervalType getAbsenceType() {
        return absenceType;
    }

    public void setAbsenceType(AbsenceIntervalType absenceType) {
        this.absenceType = absenceType;
    }

    public Instant getFromTime() {
        return fromTime;
    }

    public Instant getExpectedToTime() {
        return expectedToTime;
    }

    public Instant getActualToTime() {
        return actualToTime;
    }

    public Long getAbsenceSessionId() {
        return absenceSessionId;
    }

    public Long getTripId() {
        return tripId;
    }

    public Long getNoMedicationOrderId() {
        return noMedicationOrderId;
    }

    public void setNoMedicationOrderId(Long noMedicationOrderId) {
        this.noMedicationOrderId = noMedicationOrderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
