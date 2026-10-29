package com.eldercare.backend.staffing.shift.internal.service;

import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.staffing.api.events.ShiftEnded;
import com.eldercare.backend.staffing.api.events.ShiftStarted;
import com.eldercare.backend.staffing.internal.service.M09TimedTask;
import com.eldercare.backend.staffing.shift.internal.domain.WorkShift;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.staffing.shift.internal.service.port.ShiftHandoverState;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** T014: tác vụ của khung theo phút; chạy bù xét mọi ca tới hạn, không tạo ca hoặc bàn giao. */
@Service
@org.springframework.core.annotation.Order(20)
public class ShiftLifecycle implements M09TimedTask {
    private final ShiftStore shifts;
    private final ObjectProvider<ShiftHandoverState> handovers;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final JdbcTemplate jdbc;

    public ShiftLifecycle(
            ShiftStore shifts,
            ObjectProvider<ShiftHandoverState> handovers,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            JdbcTemplate jdbc) {
        this.shifts = shifts;
        this.handovers = handovers;
        this.audit = audit;
        this.events = events;
        this.jdbc = jdbc;
    }

    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    public void run(Instant scheduledAt, Instant actualAt) {
        for (var candidate : shifts.due(actualAt)) {
            if (!java.util.Set.of("PUBLISHED", "IN_PROGRESS", "AWAITING_HANDOVER")
                    .contains(candidate.status())) continue;
            WorkShift shift = shifts.shift(candidate.id(), true);
            if ("PUBLISHED".equals(shift.status()) && !shift.startsAt().isAfter(actualAt)) {
                change(shift, "IN_PROGRESS", "SHIFT_STARTED");
                events.publishEvent(new ShiftStarted(shift.id(), shift.scopeId(), shift.startsAt()));
                shift = shifts.shift(shift.id(), false);
            }
            if ("IN_PROGRESS".equals(shift.status()) && !shift.endsAt().isAfter(actualAt)) {
                String status = !shift.handoverRequired() || prepared(shift.id()) ? "CLOSED" : "AWAITING_HANDOVER";
                change(shift, status, "SHIFT_ENDED");
                jdbc.update(
                        "UPDATE shift_lead_history SET ends_at=? WHERE shift_id=? AND ends_at IS NULL",
                        ShiftStore.sql(shift.endsAt()),
                        shift.id());
                events.publishEvent(new ShiftEnded(shift.id(), shift.scopeId(), status, shift.endsAt()));
            } else if ("AWAITING_HANDOVER".equals(shift.status()) && prepared(shift.id())) {
                change(shift, "CLOSED", "HANDOVER_PREPARED");
            }
        }
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
                .basis(new Basis("FR-022", null, shift.id(), null))
                .details(Map.of("trigger", trigger))
                .build());
    }
}
