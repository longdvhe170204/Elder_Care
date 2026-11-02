package com.eldercare.backend.staffing.api.dto;

import java.time.Instant;

/**
 * FR-037a: shiftId rỗng khi giao Trưởng tầng không gắn ca. role giữ vai trò phân công, hoặc FLOOR_MANAGER,
 * SHIFT_MEMBER, SHIFT_LEAD để phân biệt nguồn lịch sử; assignmentId là id trong nguồn đó. Không có lý do riêng tư.
 */
public record AssignmentHistoryView(
        long assignmentId,
        long staffId,
        Long shiftId,
        String targetType,
        long targetId,
        Instant from,
        Instant to,
        String role) {}
