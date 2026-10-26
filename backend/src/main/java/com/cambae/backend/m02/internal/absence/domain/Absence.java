package com.cambae.backend.m02.internal.absence.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Lượt vắng (data-model mục 8; FR-048). {@code expectedReturnAt} là giá trị hiện hành, lịch sử ở {@code
 * expected_return_history}. Lượt đã đóng không bị sửa (FR-061): {@code departedAt}, {@code returnedAt} ở đây là bản gốc,
 * giá trị đã đính chính lấy từ {@code CorrectionApi}.
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
        boolean initialBedHold,
        Instant thresholdNotifiedAt,
        LocalDate reviewDate,
        Long previousAbsenceId,
        boolean open,
        long createdBy,
        Long closedBy) {

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
                initialBedHold,
                thresholdNotifiedAt,
                reviewDate,
                previousAbsenceId,
                open,
                createdBy,
                closedBy);
    }
}
