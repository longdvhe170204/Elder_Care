package com.eldercare.backend.staffing.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Kiểu đọc giữa module, theo 008 contracts mục 1.3/1.4; không chứa thông tin liên hệ hoặc lý do riêng tư. */
public record StaffingRatioView(
        long calculationId,
        long shiftId,
        Long floorAreaId,
        Instant calculatedAt,
        BigDecimal ratio,
        BigDecimal threshold,
        boolean passed,
        boolean everBelowThreshold,
        // 016 FR-070 (luật "Đổi api", chủ dự án duyệt 2026-10-27): số người được tính, tổng trọng số, mẫu ca, giờ ca.
        int staffCount,
        BigDecimal totalWeight,
        long shiftTemplateId,
        Instant shiftStartsAt,
        Instant shiftEndsAt) {}
