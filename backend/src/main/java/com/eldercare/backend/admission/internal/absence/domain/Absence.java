package com.eldercare.backend.admission.internal.absence.domain;

import java.time.Instant;

/**
 * Lượt vắng (data-model mục 8; FR-048; Q-395). {@code expectedReturnAt} là giá trị hiện hành, lịch sử ở {@code status_history}.
 * Lượt đã đóng không bị sửa (FR-061): {@code departedAt}, {@code returnedAt} ở đây là bản gốc, giá trị đã đính chính lấy từ {@code
 * CorrectionApi}. {@code bedReleasedAt} là lệnh Giải phóng giường của Quản lý viện (FR-055).
 */
public record Absence(
        long id,
        long residentId,
        AbsenceType type,
        String reason,
        String standardReason,
        Instant departedAt,
        Instant expectedReturnAt,
        Instant returnedAt,
        Long pickedUpById,
        Long pickupRecordId,
        String transferUnit,
        long handoverStaffId,
        boolean takesMedication,
        Instant managerNotifiedAt,
        Instant bedReleasedAt,
        Long previousAbsenceId,
        boolean open,
        long createdBy,
        Long closedBy) {

    /** Giường còn được giữ: nội trú vắng giữ giường không hạn, tới khi có lệnh Giải phóng giường (FR-050, FR-055). */
    public boolean holdBed() {
        return bedReleasedAt == null;
    }

    /** Thời điểm rời và về sau khi áp đính chính. */
    public Absence withTimes(Instant departedAt, Instant returnedAt) {
        return new Absence(
                id,
                residentId,
                type,
                reason,
                standardReason,
                departedAt,
                expectedReturnAt,
                returnedAt,
                pickedUpById,
                pickupRecordId,
                transferUnit,
                handoverStaffId,
                takesMedication,
                managerNotifiedAt,
                bedReleasedAt,
                previousAbsenceId,
                open,
                createdBy,
                closedBy);
    }
}
