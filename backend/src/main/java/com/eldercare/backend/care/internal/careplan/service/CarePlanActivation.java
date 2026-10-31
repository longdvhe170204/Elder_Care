package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.CarePlanChanged;
import com.eldercare.backend.care.internal.careplan.domain.CarePlan;
import com.eldercare.backend.care.internal.careplan.persistence.CarePlanStore;
import com.eldercare.backend.care.internal.scheduling.service.CareDailyWork;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.*;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** T015: chỉ áp dụng phiên bản đã duyệt; không lập kế hoạch hoặc công việc. */
@Component
public class CarePlanActivation implements CareDailyWork {
    private final CarePlanStore store;
    private final CareLockStore locks;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BedAllocationApi allocations;
    private final ResidentApi residents;

    public CarePlanActivation(
            CarePlanStore store,
            CareLockStore locks,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BedAllocationApi allocations,
            ResidentApi residents) {
        this.store = store;
        this.locks = locks;
        this.audit = audit;
        this.events = events;
        this.allocations = allocations;
        this.residents = residents;
    }

    @Override
    public String name() {
        return "CARE_PLAN_EFFECTIVE";
    }

    @Override
    public List<LocalDate> dueDates(LocalDate today) {
        return store.dueDates(today);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int run(LocalDate date, Instant now) {
        var candidates = store.pendingOn(date);
        locks.lock(candidates.stream().map(CarePlan::residentId).toList());
        int count = 0;
        for (var candidate : candidates) {
            var revisions = store.list(candidate.residentId(), true);
            var pending = revisions.stream()
                    .filter(p -> p.id() == candidate.id()
                            && p.status() == CarePlanStatus.PENDING_EFFECT
                            && date.equals(p.effectiveFrom()))
                    .findFirst();
            if (pending.isEmpty() || residents.isFinalStatus(candidate.residentId())) continue;
            var previous = revisions.stream()
                    .filter(p -> p.status() == CarePlanStatus.EFFECTIVE)
                    .findFirst();
            if (previous.isPresent()) {
                store.expire(previous.get().id(), date.minusDays(1));
                log(previous.get(), "EXPIRED", CarePlanStatus.EXPIRED);
            }
            store.activate(candidate.id());
            log(pending.get(), "EFFECTIVE", CarePlanStatus.EFFECTIVE);
            var floor = allocations.openAt(candidate.residentId(), now).map(BedAllocationView::floorAreaId);
            List<RecipientGroup> recipients = floor.<List<RecipientGroup>>map(id -> List.of(
                            new RecipientGroup.ShiftLead(id, new Substitution.Default()),
                            new RecipientGroup.FloorManager(id, new Substitution.Default())))
                    .orElseGet(() -> List.of(
                            new RecipientGroup.AssignedNurse(candidate.residentId(), new Substitution.Default())));
            var context =
                    new CareEventContext("M04.CARE_PLAN", candidate.id(), "EFFECTIVE:" + date, now, floor, recipients);
            events.publishEvent(
                    new CarePlanChanged(candidate.residentId(), candidate.id(), previous.map(CarePlan::id), context));
            count++;
        }
        return count;
    }

    private void log(CarePlan before, String action, CarePlanStatus after) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_PLAN_" + action, false))
                .entity("M04.CARE_PLAN", before.id())
                .before(Map.of("status", before.status()))
                .after(Map.of("status", after))
                .basis(new Basis("FR-006", null, before.id(), MDC.get(SchedulerRuns.MDC_KEY)))
                .build());
    }
}
