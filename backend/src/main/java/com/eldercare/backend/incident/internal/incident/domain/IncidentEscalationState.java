package com.eldercare.backend.incident.internal.incident.domain;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.time.Instant;

/** Phần leo thang của một sự cố đã khóa (FR-044). */
public record IncidentEscalationState(
        long id,
        Long residentId,
        Long floorAreaId,
        String status,
        IncidentLevel level,
        int escalationLevel,
        Instant acknowledgementDeadline,
        Instant maxEscalationAt,
        int maxReminderCount,
        boolean escalationStopped,
        int version) {}
