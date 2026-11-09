package com.eldercare.backend.incident.internal.scheduling.service;

import com.eldercare.backend.incident.internal.alert.service.AlertEscalationService;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Việc theo phút: leo thang cảnh báo quá hạn, nhắc khi đã tối đa (FR-033, FR-034, FR-084 a, d). */
@Component
public class AlertEscalationTask implements M05MinuteTask {
    private final AlertEscalationService escalation;

    public AlertEscalationTask(AlertEscalationService escalation) {
        this.escalation = escalation;
    }

    @Override
    public String name() {
        return "ALERT_ESCALATION";
    }

    @Override
    public int run(Instant scheduledAt, Instant actualAt) {
        return escalation.run(actualAt);
    }
}
