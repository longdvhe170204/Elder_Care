package com.eldercare.backend.staffing.api.dto;

import java.time.Instant;

/**
 * Yêu cầu đổi ca hoặc nghỉ đột xuất cho 016 (015 FR-042; contracts/module-interfaces.md mục 2.1): loại, trạng thái cuối,
 * thời gian lập và quyết định; không có lý do (FR-045).
 */
public record StaffRequestView(
        long requestId, String type, String status, Long floorAreaId, Instant filedAt, Instant decidedAt) {}
