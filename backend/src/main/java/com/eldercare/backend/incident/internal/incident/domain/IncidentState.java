package com.eldercare.backend.incident.internal.incident.domain;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;

/** Trạng thái có thể đổi của một sự cố đã khóa. */
public record IncidentState(
        long id,
        Long residentId,
        Long floorAreaId,
        String status,
        IncidentLevel level,
        boolean wasEmergency,
        int version) {}
