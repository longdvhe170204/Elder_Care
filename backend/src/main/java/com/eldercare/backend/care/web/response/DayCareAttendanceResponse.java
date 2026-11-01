package com.eldercare.backend.care.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.attendance.domain.AttendanceData;
import java.time.*;

public record DayCareAttendanceResponse(
        long id,
        long residentId,
        LocalDate date,
        AttendanceStatus status,
        LocalTime expectedArrival,
        LocalTime expectedReturn,
        LocalTime currentExpectedReturn,
        Instant arrivedAt,
        Instant leftAt,
        CheckoutType checkoutType,
        boolean adHocSession,
        boolean lateAbsenceReported) {
    public static DayCareAttendanceResponse of(SimpleRecord<AttendanceData> row) {
        var d = row.content();
        return new DayCareAttendanceResponse(
                row.id(),
                row.residentId(),
                row.effectiveFrom(),
                AttendanceStatus.valueOf(row.status()),
                d.expectedArrival(),
                d.expectedReturn(),
                d.currentExpectedReturn(),
                d.arrivedAt(),
                d.leftAt(),
                d.checkoutType(),
                d.adHocSession(),
                d.lateAbsenceId() != null);
    }
}
