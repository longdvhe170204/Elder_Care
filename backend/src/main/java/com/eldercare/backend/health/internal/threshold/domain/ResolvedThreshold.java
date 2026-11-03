package com.eldercare.backend.health.internal.threshold.domain;

import java.util.Map;

/** FR-023: phiên bản và bốn cận được chọn tại thời điểm đo. */
public record ResolvedThreshold(Long thresholdVersionId, Map<String, ThresholdBand> bands) {
    public ResolvedThreshold {
        bands = bands != null ? Map.copyOf(bands) : Map.of();
    }
}
