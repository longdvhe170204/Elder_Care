package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/** Nội dung lệnh ghi sự cố, dịch từ GhiNhanSuCo (FR-040). */
public record RecordIncidentCommand(
        Long residentId,
        long incidentTypeId,
        String source,
        Instant occurredAt,
        Instant detectedAt,
        Long floorAreaId,
        String location,
        String ongoingActivity,
        String description,
        String level,
        String lowerLevelReason,
        String initialResponse,
        boolean createDespiteExisting) {}
