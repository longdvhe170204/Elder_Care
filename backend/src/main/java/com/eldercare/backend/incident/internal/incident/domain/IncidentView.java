package com.eldercare.backend.incident.internal.incident.domain;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.time.Instant;

/** Một dòng sự cố đọc cho màn hình (api-007 {@code Incident}): nội dung bất biến cộng trạng thái xử lý. */
public record IncidentView(
        long id,
        Long residentId,
        long incidentTypeId,
        long currentTypeId,
        String context,
        Instant occurredAt,
        Instant discoveredAt,
        Long floorAreaId,
        String location,
        String ongoingActivity,
        String description,
        String initialResponse,
        IncidentLevel initialLevel,
        String lowerLevelReason,
        Long sourceAlertId,
        boolean outOfScope,
        long recordedBy,
        String status,
        IncidentLevel level,
        boolean wasEmergency,
        int escalationLevel,
        Long assigneeId,
        Instant receivedAt,
        boolean noOnDutyDoctor,
        String resolution) {}
