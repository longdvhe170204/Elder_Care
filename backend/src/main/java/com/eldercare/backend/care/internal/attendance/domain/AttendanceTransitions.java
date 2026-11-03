package com.eldercare.backend.care.internal.attendance.domain;

import com.eldercare.backend.care.api.dto.AttendanceStatus;
import java.util.*;

/** FR-013: luật chuyển thuần; service kiểm nguồn, người thực hiện và thời gian. */
public final class AttendanceTransitions {
    private AttendanceTransitions() {}

    private static final Map<String, Map<AttendanceStatus, AttendanceStatus>> TABLE = Map.of(
            "ARRIVE",
                    Map.of(
                            AttendanceStatus.NOT_ARRIVED,
                            AttendanceStatus.PRESENT,
                            AttendanceStatus.UNREPORTED_ABSENT,
                            AttendanceStatus.PRESENT,
                            AttendanceStatus.HOLIDAY_CANCELLED,
                            AttendanceStatus.PRESENT),
            "LEAVE", Map.of(AttendanceStatus.PRESENT, AttendanceStatus.LEFT),
            "HOLIDAY",
                    Map.of(
                            AttendanceStatus.NOT_ARRIVED,
                            AttendanceStatus.HOLIDAY_CANCELLED,
                            AttendanceStatus.UNREPORTED_ABSENT,
                            AttendanceStatus.HOLIDAY_CANCELLED),
            "REPORT_ABSENCE", Map.of(AttendanceStatus.NOT_ARRIVED, AttendanceStatus.REPORTED_ABSENT),
            "NO_SHOW", Map.of(AttendanceStatus.NOT_ARRIVED, AttendanceStatus.UNREPORTED_ABSENT));

    public static Optional<AttendanceStatus> next(AttendanceStatus status, String event) {
        return Optional.ofNullable(TABLE.getOrDefault(event, Map.of()).get(status));
    }
}
