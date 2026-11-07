package com.eldercare.backend.health.internal.threshold.domain;

import com.eldercare.backend.common.Reason;
import java.time.Instant;
import java.util.Map;

/** Lệnh "Thiết lập ngưỡng" (FR-020): phạm vi {@code FACILITY} hoặc {@code PERSONAL}, các thành phần theo mã. */
public record NewThreshold(
        String scope,
        Long residentId,
        long indicatorId,
        Instant effectiveFrom,
        Map<String, ThresholdBand> components,
        Reason reason) {}
