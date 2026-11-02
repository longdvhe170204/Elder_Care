package com.eldercare.backend.health.internal.threshold.service;

import com.eldercare.backend.health.internal.threshold.domain.ResolvedThreshold;
import java.time.Instant;

/** Tra cứu phiên bản ngưỡng áp dụng tại thời điểm đo (FR-023, FR-024). */
public interface ApplicableThresholdResolver {
    ResolvedThreshold resolve(long residentId, long indicatorId, String indicatorCode, Instant measuredAt);
}
