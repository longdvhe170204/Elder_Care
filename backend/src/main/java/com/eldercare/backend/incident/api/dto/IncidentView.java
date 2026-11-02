package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/** Nguồn đọc sự cố cho module khác; không phải phản hồi HTTP (mục 1.7). */
public record IncidentView(
        long id, Long residentId, String type, String level, String status, Instant createdAt, Long floorAreaId) {}
