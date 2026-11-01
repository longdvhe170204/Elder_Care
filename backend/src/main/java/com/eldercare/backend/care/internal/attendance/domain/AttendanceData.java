package com.eldercare.backend.care.internal.attendance.domain;

import com.eldercare.backend.care.api.dto.CheckoutType;
import java.time.*;

/** Nội dung typed của simple_record; trạng thái/người/ngày lấy từ cột chung. */
public record AttendanceData(
        long floorAreaId,
        LocalTime expectedArrival,
        LocalTime expectedReturn,
        LocalTime currentExpectedReturn,
        Long returnRegistrationId,
        Long arrivalRecordId,
        Long departureRecordId,
        Instant arrivedAt,
        Instant leftAt,
        CheckoutType checkoutType,
        boolean adHocSession,
        Long timelyAbsenceId,
        Long lateAbsenceId) {
    public AttendanceData calendar(LocalTime arrival, LocalTime departure, Long timely, Long late) {
        return new AttendanceData(
                floorAreaId,
                arrival,
                departure,
                returnRegistrationId == null ? departure : currentExpectedReturn,
                returnRegistrationId,
                arrivalRecordId,
                departureRecordId,
                arrivedAt,
                leftAt,
                checkoutType,
                adHocSession,
                timely,
                late);
    }

    public AttendanceData arrive(long record, Instant at, boolean adHoc) {
        return new AttendanceData(
                floorAreaId,
                expectedArrival,
                expectedReturn,
                currentExpectedReturn,
                returnRegistrationId,
                record,
                departureRecordId,
                at,
                leftAt,
                checkoutType,
                adHoc,
                timelyAbsenceId,
                lateAbsenceId);
    }

    public AttendanceData leave(long record, Instant at, CheckoutType type) {
        return new AttendanceData(
                floorAreaId,
                expectedArrival,
                expectedReturn,
                currentExpectedReturn,
                returnRegistrationId,
                arrivalRecordId,
                record,
                arrivedAt,
                at,
                type,
                adHocSession,
                timelyAbsenceId,
                lateAbsenceId);
    }

    public AttendanceData returnTime(LocalTime time, Long registration) {
        return new AttendanceData(
                floorAreaId,
                expectedArrival,
                expectedReturn,
                time,
                registration,
                arrivalRecordId,
                departureRecordId,
                arrivedAt,
                leftAt,
                checkoutType,
                adHocSession,
                timelyAbsenceId,
                lateAbsenceId);
    }
}
