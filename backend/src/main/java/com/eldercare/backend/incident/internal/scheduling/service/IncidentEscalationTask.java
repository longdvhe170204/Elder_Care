package com.eldercare.backend.incident.internal.scheduling.service;

import com.eldercare.backend.incident.internal.incident.service.IncidentEscalationService;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Việc theo phút: leo thang sự cố quá hạn tiếp nhận, nhắc khi đã tối đa (FR-044, research mục việc theo phút M05). */
@Component
public class IncidentEscalationTask implements M05MinuteTask {
    private final IncidentEscalationService escalation;

    public IncidentEscalationTask(IncidentEscalationService escalation) {
        this.escalation = escalation;
    }

    @Override
    public String name() {
        return "INCIDENT_ESCALATION";
    }

    @Override
    public int run(Instant scheduledAt, Instant actualAt) {
        return escalation.run(actualAt);
    }
}
