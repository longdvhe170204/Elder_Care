package com.eldercare.backend.staffing.ratio.internal.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** FR-037: bản lưu bất biến theo lần tính, tách khỏi trạng thái cảnh báo của ca. */
public record StaffingRatioCalculation(
        long id,
        long shiftId,
        Instant at,
        String event,
        BigDecimal totalWeight,
        int staffCount,
        BigDecimal ratio,
        BigDecimal threshold,
        boolean meetsThreshold) {}
