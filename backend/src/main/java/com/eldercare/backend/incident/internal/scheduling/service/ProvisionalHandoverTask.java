package com.eldercare.backend.incident.internal.scheduling.service;

import com.eldercare.backend.incident.internal.alert.service.AlertProvisionalTakeoverService;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Việc theo phút: tạm nhận khi ca sau đã bắt đầu mà bàn giao chưa xác nhận (FR-037, Q-94). */
@Component
public class ProvisionalHandoverTask implements M05MinuteTask {
    private final AlertProvisionalTakeoverService takeover;

    public ProvisionalHandoverTask(AlertProvisionalTakeoverService takeover) {
        this.takeover = takeover;
    }

    @Override
    public String name() {
        return "PROVISIONAL_HANDOVER";
    }

    @Override
    public int run(Instant scheduledAt, Instant actualAt) {
        return takeover.run(actualAt);
    }
}
