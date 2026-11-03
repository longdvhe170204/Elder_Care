package com.eldercare.backend.care.internal.attendance.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

public final class AttendanceKinds {
    public static final SharedKind<AttendanceData> DAYS =
            SharedKind.of("M04.DAY_CARE_ATTENDANCE", AttendanceData.class).notDeletable();
    public static final SharedKind<AttendanceRecordData> RECORDS =
            SharedKind.of("M04.ATTENDANCE_RECORD", AttendanceRecordData.class).notDeletable();

    private AttendanceKinds() {}
}
