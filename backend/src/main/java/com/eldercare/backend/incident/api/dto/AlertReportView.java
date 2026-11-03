package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/** Mốc tiếp nhận và tới nơi giữ nguyên để 016 tính chỉ tiêu (FR-081, Q-374/375). */
public record AlertReportView(
        long alertId,
        Long residentId,
        String level,
        String levelAtAcknowledgement,
        Instant firstAt,
        Instant acknowledgedAt,
        int escalationCount,
        boolean maximumEscalation,
        String type,
        Long floorAreaId,
        String source,
        Instant arrivedAt,
        boolean arrivalTimeMissing) {}
