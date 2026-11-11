package com.eldercare.backend.care.api.dto;

import java.time.Instant;

/**
 * 014 cho 016 FR-037 (Q-168, Q-254): công việc hoàn thành của ca không vào mẫu kiểm tra chất lượng vì do chính người kiểm
 * tra thực hiện; {@code performerRole} là {@code FLOOR_MANAGER} (Trưởng tầng được giao) hoặc {@code SHIFT_LEAD} (Người
 * phụ trách ca của ca không có Trưởng tầng được giao).
 */
public record OutOfCheckScopeView(
        long careTaskId,
        long shiftId,
        long floorAreaId,
        long performerStaffId,
        String performerRole,
        Instant performedAt) {}
