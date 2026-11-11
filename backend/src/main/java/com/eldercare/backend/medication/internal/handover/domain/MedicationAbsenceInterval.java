package com.eldercare.backend.medication.internal.handover.domain;

import java.time.Instant;

/**
 * Khoảng vắng ảnh hưởng liều (BR-M07-03): dòng {@code simple_record} loại {@code M07.ABSENCE_INTERVAL} (006 T069,
 * {@code V006_98}); một khoảng mở mỗi người qua {@code unique_key} {@code M07.AI:<residentId>}.
 */
public class MedicationAbsenceInterval {

    private Long id;

    private Long residentId;

    private AbsenceIntervalType absenceType;

    private Instant fromTime;

    private Instant expectedToTime;

    private Instant actualToTime;

    private Long absenceSessionId;

    private Long tripId;

    private Long noMedicationOrderId;

    private Instant createdAt;

    protected MedicationAbsenceInterval() {}

    public MedicationAbsenceInterval(
            Long residentId,
            AbsenceIntervalType absenceType,
            Instant fromTime,
            Instant expectedToTime,
            Long absenceSessionId,
            Long tripId,
            Instant now) {
        this.residentId = residentId;
        this.absenceType = absenceType;
        this.fromTime = fromTime;
        this.expectedToTime = expectedToTime;
        this.absenceSessionId = absenceSessionId;
        this.tripId = tripId;
        this.createdAt = now;
    }

    public void close(Instant actualToTime) {
        this.actualToTime = actualToTime;
    }

    /** 014 FR-031a (c): Hủy ghi nhận về, người cao tuổi chưa về nên khoảng vắng mở lại. */
    public void reopen() {
        this.actualToTime = null;
    }

    public void extend(Instant newExpectedToTime) {
        this.expectedToTime = newExpectedToTime;
    }

    /** Khoảng đi của chuyến đổi do đính chính thời điểm rời, về (FR-048b): đã về thì đổi giờ về thực tế. */
    public void reschedule(Instant newFromTime, Instant newToTime) {
        this.fromTime = newFromTime;
        if (isOpen()) {
            this.expectedToTime = newToTime;
        } else {
            this.actualToTime = newToTime;
        }
    }

    public boolean isOpen() {
        return this.actualToTime == null;
    }

    // --- Getters & Setters ---

    private int version;

    /** Gán id, phiên bản của dòng bảng chung sau khi đọc hoặc ghi. */
    public void restoreRow(long id, int version, Instant createdAt) {
        this.id = id;
        this.version = version;
        this.createdAt = createdAt;
    }

    public static MedicationAbsenceInterval restore(long residentId, AbsenceIntervalContent c) {
        MedicationAbsenceInterval a = new MedicationAbsenceInterval(
                residentId, c.absenceType(), c.fromTime(), c.expectedToTime(), c.absenceSessionId(), c.tripId(), null);
        a.actualToTime = c.actualToTime();
        a.noMedicationOrderId = c.noMedicationOrderId();
        return a;
    }

    public AbsenceIntervalContent content() {
        return new AbsenceIntervalContent(
                absenceType,
                fromTime,
                expectedToTime,
                actualToTime,
                absenceSessionId,
                tripId,
                noMedicationOrderId,
                null);
    }

    public int getVersion() {
        return version;
    }

    public Long getId() {
        return id;
    }

    public Long getResidentId() {
        return residentId;
    }

    public AbsenceIntervalType getAbsenceType() {
        return absenceType;
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

    /** Chuyến đi không mang thuốc theo lệnh đã ghi (FR-048b). */
    public void attachNoMedicationOrder(Long noMedicationOrderId) {
        this.absenceType = AbsenceIntervalType.TRIP_NO_MED;
        this.noMedicationOrderId = noMedicationOrderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
