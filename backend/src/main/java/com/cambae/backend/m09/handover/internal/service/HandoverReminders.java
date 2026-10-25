package com.cambae.backend.m09.handover.internal.service;

import com.cambae.backend.m09.api.events.*;
import com.cambae.backend.m09.handover.internal.persistence.HandoverStore;
import com.cambae.backend.m09.internal.service.M09TimedTask;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m15.auditlog.api.*;
import com.cambae.backend.m15.parameter.api.*;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** FR-039/043: chỉ nhắc, chạy bù một lần mỗi mốc; không lập bàn giao hoặc mục. */
@Service
@org.springframework.core.annotation.Order(40)
public class HandoverReminders implements M09TimedTask {
    private final HandoverStore store;
    private final ShiftStore shifts;
    private final HandoverService service;
    private final ParameterApi parameters;
    private final HandoverNotifications notifications;
    private final ApplicationEventPublisher events;
    private final AuditLogApi audit;

    public HandoverReminders(
            HandoverStore store,
            ShiftStore shifts,
            HandoverService service,
            ParameterApi parameters,
            HandoverNotifications notifications,
            ApplicationEventPublisher events,
            AuditLogApi audit) {
        this.store = store;
        this.shifts = shifts;
        this.service = service;
        this.parameters = parameters;
        this.notifications = notifications;
        this.events = events;
        this.audit = audit;
    }

    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    public void run(Instant scheduled, Instant now) {
        var confirmDelay = parameters.read(ParameterCode.CFG_M09_05).asDuration();
        for (long id : store.reminderCandidates(now)) {
            var s = shifts.shift(id, true);
            var handover = store.forShift(id);
            if (handover.filter(h -> "CONFIRMED".equals(h.status())).isPresent()) continue;
            if (handover.isEmpty()
                    && !now.isBefore(service.preparationStarts(s))
                    && store.claimReminder(id, "prepare_handover_reminded_at", now)) {
                var request = notifications.reminder(
                        "HANDOVER_NOT_PREPARED", id, "M09.SHIFT", s, now, "Đã tới mốc lập bàn giao ca", true);
                events.publishEvent(new HandoverNotPrepared(id, now, request));
                log(id, "PREPARATION", now);
            }
            boolean unfinished = handover.isEmpty()
                    || Set.of("DRAFT", "HAS_COMMENTS").contains(handover.get().status());
            if (unfinished && !now.isBefore(s.endsAt()) && store.claimReminder(id, "shift_end_reminded_at", now)) {
                if (handover.isPresent())
                    events.publishEvent(new HandoverIncompleteAtShiftEnd(
                            handover.get().id(),
                            now,
                            notifications.reminder(
                                    "HANDOVER_INCOMPLETE_AT_SHIFT_END",
                                    handover.get().id(),
                                    "M09.HANDOVER",
                                    s,
                                    now,
                                    "Ca đã kết thúc, bàn giao chưa lập xong",
                                    true)));
                else
                    events.publishEvent(new HandoverNotPrepared(
                            id,
                            now,
                            notifications.reminder(
                                    "HANDOVER_NOT_PREPARED",
                                    id,
                                    "M09.SHIFT",
                                    s,
                                    now,
                                    "Ca đã kết thúc, chưa có bàn giao",
                                    true)));
                log(id, "SHIFT_END", now);
            }
            var next = service.next(s);
            if (next.isPresent()
                    && !now.isBefore(next.get().startsAt().plus(confirmDelay))
                    && store.claimReminder(id, "unconfirmed_handover_reminded_at", now)) {
                if (handover.isPresent())
                    events.publishEvent(new HandoverNotConfirmed(
                            handover.get().id(),
                            now,
                            notifications.reminder(
                                    "HANDOVER_NOT_CONFIRMED",
                                    handover.get().id(),
                                    "M09.HANDOVER",
                                    s,
                                    now,
                                    "Ca sau đã bắt đầu, bàn giao chưa được xác nhận",
                                    false)));
                else
                    events.publishEvent(new HandoverNotPrepared(
                            id,
                            now,
                            notifications.reminder(
                                    "HANDOVER_NOT_PREPARED",
                                    id,
                                    "M09.SHIFT",
                                    s,
                                    now,
                                    "Ca sau đã bắt đầu, chưa có bàn giao để xác nhận",
                                    false)));
                log(id, "UNCONFIRMED", now);
            }
        }
    }

    private void log(long shift, String milestone, Instant now) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("HANDOVER_REMINDER_SENT", false))
                .entity("M09.SHIFT", shift)
                .basis(new Basis("FR-043", null, shift, null))
                .details(Map.of("milestone", milestone, "at", now.toString()))
                .build());
    }
}
