package com.eldercare.backend.incident.internal.incident.domain;

import java.time.Instant;

/** Thông tin sự cố nghi nhiễm cần khóa khi lập danh sách tiếp xúc. */
public record SuspectedIncident(long id, long residentId, Instant discoveredAt, String status, Long floorAreaId) {}
