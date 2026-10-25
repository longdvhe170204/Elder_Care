package com.cambae.backend.m03.internal.bed.service;

import com.cambae.backend.common.notification.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
import com.cambae.backend.m03.api.events.*;
import com.cambae.backend.m03.internal.allocation.persistence.AllocationStore;
import com.cambae.backend.m03.internal.bed.persistence.BedReadinessStore;
import com.cambae.backend.m15.auditlog.api.*;
import com.cambae.backend.m15.parameter.api.*;
import java.time.Duration;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T059: kiểm lại giường sau khóa; đánh dấu, audit và sự kiện cùng rollback. */
@Service
public class BedReadinessReminderProcessor {
    private final BedReadinessStore store;
    private final AllocationStore locks;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;

    public BedReadinessReminderProcessor(
            BedReadinessStore store,
            AllocationStore locks,
            BusinessTime time,
            ParameterApi parameters,
            AuditLogApi audit,
            ApplicationEventPublisher events) {
        this.store = store;
        this.locks = locks;
        this.time = time;
        this.parameters = parameters;
        this.audit = audit;
        this.events = events;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean remind(long bed, long run) {
        locks.guardWrites(true);
        locks.guardRooms(List.of(bed));
        locks.lockBed(bed);
        var rows = store.overdue(bed, time.now());
        if (rows.isEmpty()) return false;
        var row = rows.getFirst();
        var now = time.now();
        boolean late = Duration.between(row.deadline(), now)
                        .compareTo(parameters.read(ParameterCode.CFG_M03_08).asDuration())
                > 0;
        store.mark(bed, now);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_REMINDER", false))
                .entity("M03.BED", bed)
                .basis(new Basis("FR-044", null, bed, run))
                .details(Map.of("plannedAt", row.deadline().toString(), "executedLate", late))
                .build());
        events.publishEvent(new BedReadinessReminder(
                bed,
                row.deadline(),
                new BedNotificationContext(
                        now,
                        row.floorId(),
                        List.of(new RecipientGroup.FloorManager(row.floorId(), new Substitution.Default())))));
        if (late)
            events.publishEvent(new DelayedScheduledWork(
                    "BED_READINESS_REMINDER",
                    bed,
                    row.deadline(),
                    now,
                    new BedNotificationContext(
                            now,
                            row.floorId(),
                            List.of(new RecipientGroup.FacilityRole(
                                    com.cambae.backend.common.Role.NURSING_HOME_MANAGER)))));
        return true;
    }
}
