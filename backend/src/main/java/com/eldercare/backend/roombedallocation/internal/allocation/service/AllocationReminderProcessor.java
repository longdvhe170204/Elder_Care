package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import com.eldercare.backend.roombedallocation.api.events.*;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationReminderStore;
import java.time.Duration;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T059: dấu gỡ thì xóa mốc kỹ thuật; không tạo yêu cầu/phân bổ thay actor. */
@Service
public class AllocationReminderProcessor {
    private final AllocationReminderStore store;
    private final AllocationLocks locks;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;

    public AllocationReminderProcessor(
            AllocationReminderStore store,
            AllocationLocks locks,
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
    public boolean remind(AllocationReminderStore.Key key, String run) {
        var resident = store.resident(key);
        if (resident == null) {
            store.delete(key);
            return false;
        }
        locks.lock(resident, List.of());
        var source = store.source(key);
        if (source == null || !source.open()) {
            store.delete(key);
            return false;
        }
        var now = time.now();
        var marker = store.marker(key, now);
        var planned = (marker.last() == null ? marker.started() : marker.last())
                .plus(parameters.read(ParameterCode.CFG_M03_02).asDuration());
        if (now.isBefore(planned)) return false;
        boolean late = Duration.between(planned, now)
                        .compareTo(parameters.read(ParameterCode.CFG_M03_08).asDuration())
                > 0;
        store.mark(key, now);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_REMINDER", false))
                .entity("M03." + key.type(), key.id())
                .resident(resident)
                .basis(new Basis("CFG-M03-02", null, key.id(), run))
                .details(Map.of("flagType", key.flag(), "plannedAt", planned.toString(), "executedLate", late))
                .build());
        List<RecipientGroup> recipients = new ArrayList<>();
        recipients.add(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        if (source.floor() != null
                && List.of("ROOM_UNSUITABLE", "AWAITING_SAME_PRICE").contains(key.flag()))
            recipients.add(new RecipientGroup.FloorManager(source.floor(), new Substitution.Default()));
        events.publishEvent(new AllocationReminder(
                key.flag(), key.id(), marker.count() + 1, new BedNotificationContext(now, source.floor(), recipients)));
        if (late)
            events.publishEvent(new DelayedScheduledWork(
                    "ALLOCATION_REMINDER",
                    key.id(),
                    planned,
                    now,
                    new BedNotificationContext(
                            now, source.floor(), List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))));
        return true;
    }
}
