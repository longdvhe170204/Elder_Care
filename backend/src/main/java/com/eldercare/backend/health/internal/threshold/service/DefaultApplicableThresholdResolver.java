package com.eldercare.backend.health.internal.threshold.service;

import com.eldercare.backend.health.internal.threshold.domain.ResolvedThreshold;
import com.eldercare.backend.health.internal.threshold.persistence.ApplicableThresholdStore;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** FR-023, FR-024: chỉ đọc ngưỡng đã thiết lập, giữ phiên bản tại thời điểm đo. */
@Service
public class DefaultApplicableThresholdResolver implements ApplicableThresholdResolver {
    private final ApplicableThresholdStore thresholds;

    public DefaultApplicableThresholdResolver(ApplicableThresholdStore thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public ResolvedThreshold resolve(long residentId, long indicatorId, String indicatorCode, Instant measuredAt) {
        return thresholds.resolve(residentId, indicatorId, measuredAt);
    }
}
