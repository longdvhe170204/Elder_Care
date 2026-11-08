package com.eldercare.backend.health.internal.threshold.service;

import com.eldercare.backend.health.internal.scheduling.service.M06TimedTask;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Việc theo phút của M06: ngưỡng Chờ hiệu lực tới thời điểm hiệu lực thì thành Hiệu lực (bảng FR-022, S-07). */
@Component
public class ThresholdActivationTask implements M06TimedTask {
    private final ThresholdService thresholds;

    public ThresholdActivationTask(ThresholdService thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public String name() {
        return "THRESHOLD_ACTIVATION";
    }

    @Override
    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    @Override
    public int run(Instant scheduledAt, Instant actualAt) {
        return thresholds.activateDue(actualAt);
    }
}
