package com.cambae.backend.m09.api.dto;

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
        boolean everBelowThreshold) {}
