package com.eldercare.backend.health.internal.threshold.domain;

import java.math.BigDecimal;

/** Bốn cận của một thành phần chỉ số; cận {@code null} không được dùng để so phía tương ứng (FR-024). */
public record ThresholdBand(
        BigDecimal lowerDangerous, BigDecimal lowerWarning, BigDecimal upperWarning, BigDecimal upperDangerous) {

    public static ThresholdBand none() {
        return new ThresholdBand(null, null, null, null);
    }

    public boolean hasAnyBound() {
        return lowerDangerous != null || lowerWarning != null || upperWarning != null || upperDangerous != null;
    }
}
