package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/**
 * Nguồn đọc sự cố cho module khác; không phải phản hồi HTTP (mục 1.7). {@code assigneeStaffId} là mã nhân viên của
 * người xử lý hiện tại, rỗng khi chưa giao (013 FR-011a; "Đổi api" 2026-11-08, chờ chủ dự án duyệt trong PR).
 */
public record IncidentView(
        long id,
        Long residentId,
        String type,
        String level,
        String status,
        Instant createdAt,
        Long floorAreaId,
        Long assigneeStaffId) {
    /** Giữ lời gọi cũ (bản giả, test của module khác): chưa có người xử lý. */
    public IncidentView(
            long id, Long residentId, String type, String level, String status, Instant createdAt, Long floorAreaId) {
        this(id, residentId, type, level, status, createdAt, floorAreaId, null);
    }
}
