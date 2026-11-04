package com.eldercare.backend.incident.internal.incident.domain;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.time.Instant;

/** Nội dung một dòng sự cố mới khi ghi nhận (FR-040, T034). */
public record NewIncident(
        Long residentId,
        long typeId,
        String typeCode,
        String context,
        Instant occurredAt,
        Instant discoveredAt,
        Long floorAreaId,
        String location,
        String ongoingActivity,
        String description,
        String initialResponse,
        IncidentLevel level,
        String lowerLevelReason,
        boolean outOfScope,
        long recordedBy,
        Instant recordedAt,
        Instant acknowledgementDeadline,
        boolean noOnDutyDoctor,
        Long sourceAlertId) {
    public NewIncident(
            Long residentId,
            long typeId,
            String typeCode,
            String context,
            Instant occurredAt,
            Instant discoveredAt,
            Long floorAreaId,
            String location,
            String ongoingActivity,
            String description,
            String initialResponse,
            IncidentLevel level,
            String lowerLevelReason,
            boolean outOfScope,
            long recordedBy,
            Instant recordedAt,
            Instant acknowledgementDeadline,
            boolean noOnDutyDoctor) {
        this(
                residentId,
                typeId,
                typeCode,
                context,
                occurredAt,
                discoveredAt,
                floorAreaId,
                location,
                ongoingActivity,
                description,
                initialResponse,
                level,
                lowerLevelReason,
                outOfScope,
                recordedBy,
                recordedAt,
                acknowledgementDeadline,
                noOnDutyDoctor,
                null);
    }
}
