package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/** Khoảng đang theo dõi sau ngã hoặc tiếp xúc (FR-081). */
public record MonitoringView(long residentId, String type, Long floorAreaId, Instant startsAt, Instant endsAt) {}
