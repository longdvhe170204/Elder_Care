package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/** Sự cố chưa hủy phục vụ thống kê, FR-081. */
public record IncidentReportView(
        long incidentId,
        Long residentId,
        String level,
        String type,
        boolean nonHealth,
        Long floorAreaId,
        Instant createdAt,
        Instant acknowledgedAt,
        int escalationCount) {}
