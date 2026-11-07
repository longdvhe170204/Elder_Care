package com.eldercare.backend.incident.internal.scheduling.service;

import com.eldercare.backend.incident.internal.incident.service.PostFallMonitoringService;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Việc theo phút: khoảng theo dõi sau ngã hết thời gian sang Đã hết (FR-052, research S-14). */
@Component
public class PostFallExpiryTask implements M05MinuteTask {
    private final PostFallMonitoringService monitoring;

    public PostFallExpiryTask(PostFallMonitoringService monitoring) {
        this.monitoring = monitoring;
    }

    @Override
    public String name() {
        return "POST_FALL_EXPIRY";
    }

    @Override
    public int run(Instant scheduledAt, Instant actualAt) {
        return monitoring.expireDue(actualAt);
    }
}
