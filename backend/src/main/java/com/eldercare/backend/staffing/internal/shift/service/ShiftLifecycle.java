package com.eldercare.backend.staffing.internal.shift.service;

import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.staffing.api.events.ShiftEnded;
import com.eldercare.backend.staffing.api.events.ShiftStarted;
import com.eldercare.backend.staffing.internal.service.M09TimedTask;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** T014: tác vụ của khung theo phút; chạy bù xét mọi ca tới hạn, không tạo ca hoặc bàn giao. */
@Service
@org.springframework.core.annotation.Order(20)
public class ShiftLifecycle implements M09TimedTask {
    private final ShiftStore shifts;
    private final ObjectProvider<ShiftHandoverState> handovers;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;

    public ShiftLifecycle(
            ShiftStore shifts,
            ObjectProvider<ShiftHandoverState> handovers,
            AuditLogApi audit,
            ApplicationEventPublisher events) {
        this.shifts = shifts;
        this.handovers = handovers;
        this.audit = audit;
        this.events = events;
    }

    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    public int run(Instant scheduledAt, Instant actualAt) {
        int processed = 0;
        for (var candidate : shifts.due(actualAt)) {
            if (!java.util.Set.of("PUBLISHED", "IN_PROGRESS", "AWAITING_HANDOVER")
                    .contains(candidate.status())) continue;
            WorkShift shift = shifts.shift(candidate.id(), true);
            boolean handled = false;
            if ("PUBLISHED".equals(shift.status()) && !shift.startsAt().isAfter(actualAt)) {
                handled = true;
                change(shift, "IN_PROGRESS", "SHIFT_STARTED");
                events.publishEvent(new ShiftStarted(shift.id(), shift.scopeId(), shift.startsAt()));
                shift = shifts.shift(shift.id(), false);
            }
            if ("IN_PROGRESS".equals(shift.status()) && !shift.endsAt().isAfter(actualAt)) {
                String status = !shift.handoverRequired() || prepared(shift.id()) ? "CLOSED" : "AWAITING_HANDOVER";
                handled = true;
                change(shift, status, "SHIFT_ENDED");
                shifts.closeOpenLead(shift.id(), shift.endsAt());
                events.publishEvent(new ShiftEnded(shift.id(), shift.scopeId(), status, shift.endsAt()));
            } else if ("AWAITING_HANDOVER".equals(shift.status()) && prepared(shift.id())) {
                handled = true;
                change(shift, "CLOSED", "HANDOVER_PREPARED");
            }
            if (handled) processed++;
        }
        return processed;
    }

    private boolean prepared(long id) {
        return handovers.orderedStream().anyMatch(reader -> reader.prepared(id));
    }

    private void change(WorkShift shift, String status, String trigger) {
        shifts.transition(shift.id(), status);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("SHIFT_STATUS_CHANGED", false))
                .entity("M09.SHIFT", shift.id())
                .before(Map.of("status", shift.status()))
                .after(Map.of("status", status))
                .basis(new Basis("FR-022", null, shift.id(), MDC.get(SchedulerRuns.MDC_KEY)))
                .details(Map.of("trigger", trigger))
                .build());
    }
}
