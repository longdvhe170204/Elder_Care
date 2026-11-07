package com.eldercare.backend.staffing.internal.shift.domain;

import java.time.Instant;

public record ShiftMember(
        long staffId,
        boolean onDutyDoctor,
        Instant absentAt,
        Instant withdrawnAt,
        Instant addedAt,
        Long approvedLeaveRequestId,
        Instant leftAt) {
    public boolean presentAt(Instant at) {
        return approvedLeaveRequestId == null
                && (absentAt == null || absentAt.isAfter(at))
                && (withdrawnAt == null || withdrawnAt.isAfter(at))
                && (leftAt == null || leftAt.isAfter(at))
                && (addedAt == null || !addedAt.isAfter(at));
    }
}
