package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/**
 * Sự cố chưa hủy phục vụ thống kê, FR-081. {@code levelAtAcknowledgement}: mức lúc tiếp nhận xử lý, rỗng khi chưa tiếp
 * nhận; {@code status}: trạng thái hiện hành ({@code NEW}, {@code IN_PROGRESS}, {@code MONITORING}, {@code CLOSED})
 * (007 T108, "Đổi api" duyệt 2026-11-08; 016 FR-042, FR-042a, FR-020).
 */
public record IncidentReportView(
        long incidentId,
        Long residentId,
        String level,
        String levelAtAcknowledgement,
        String type,
        boolean nonHealth,
        Long floorAreaId,
        Instant createdAt,
        Instant acknowledgedAt,
        int escalationCount,
        String status) {}
