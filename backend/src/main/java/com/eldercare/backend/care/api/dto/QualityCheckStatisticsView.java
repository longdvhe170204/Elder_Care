package com.eldercare.backend.care.api.dto;

import java.time.Instant;

/**
 * 014 ActivityStatisticsApi.qualityChecks (FR-065); result PASS, FAIL, NOT_CHECKABLE, OVERDUE. {@code performerStaffId}
 * là nhân viên thực hiện công việc gốc (tỷ lệ Đạt theo nhân viên; Đổi api 07/11/2026, chủ dự án duyệt), rỗng khi nguồn
 * ca không có hồ sơ nhân viên.
 */
public record QualityCheckStatisticsView(
        long itemId,
        long shiftId,
        long floorAreaId,
        long careTaskId,
        String result,
        String checkerRole,
        Long performerStaffId,
        Instant checkedAt) {}
