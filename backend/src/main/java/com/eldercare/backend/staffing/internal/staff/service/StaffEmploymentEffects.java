package com.eldercare.backend.staffing.internal.staff.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.care.api.ShiftTaskApi;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.events.*;
import com.eldercare.backend.staffing.internal.assignment.persistence.AssignmentStore;
import com.eldercare.backend.staffing.internal.assignment.service.AssignmentSources;
import com.eldercare.backend.staffing.internal.duty.service.DepartmentDutyService;
import com.eldercare.backend.staffing.internal.service.M09TimedTask;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.staff.persistence.*;
import java.time.*;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** T033: tác động nguyên tử theo lệnh đã ghi, không khôi phục ca/phân công khi nhận lại. */
@Service
@org.springframework.core.annotation.Order(5)
public class StaffEmploymentEffects implements M09TimedTask {
    private final StaffStore staff;
    private final StaffEmploymentStore employment;
    private final ShiftStore shifts;
    private final AssignmentStore assignments;
    private final AssignmentSources sources;
    private final DepartmentDutyService departments;
    private final AccountApi accounts;
    private final ObjectProvider<ShiftTaskApi> tasks;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final StaffNotifications notifications;
    private final com.eldercare.backend.staffing.internal.ratio.service.StaffingRatioService ratios;

    public StaffEmploymentEffects(
            StaffStore staff,
            StaffEmploymentStore employment,
            ShiftStore shifts,
            AssignmentStore assignments,
            AssignmentSources sources,
            DepartmentDutyService departments,
            AccountApi accounts,
            ObjectProvider<ShiftTaskApi> tasks,
            AuditLogApi audit,
            BusinessTime time,
            ApplicationEventPublisher events,
            StaffNotifications notifications,
            com.eldercare.backend.staffing.internal.ratio.service.StaffingRatioService ratios) {
        this.staff = staff;
        this.employment = employment;
        this.shifts = shifts;
        this.assignments = assignments;
        this.sources = sources;
        this.departments = departments;
        this.accounts = accounts;
        this.tasks = tasks;
        this.audit = audit;
        this.time = time;
        this.events = events;
        this.notifications = notifications;
        this.ratios = ratios;
    }

    public java.util.Optional<com.eldercare.backend.administration.account.api.dto.AccountView> account(long id) {
        return accounts.byStaff(id);
    }

    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    public int run(Instant scheduled, Instant actual) {
        int processed = 0;
        for (long id : employment.due(actual)) {
            staff.lock(id);
            apply(id, employment.effectiveAt(id));
            processed++;
        }
        return processed;
    }

    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void apply(long id, Instant at) {
        var person = staff.lock(id);
        if (!"WORKING".equals(person.status())) return;
        if (departments.isLead(id))
            throw new com.eldercare.backend.common.error.BusinessException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "REPLACEMENT_LEAD_REQUIRED",
                    "Cần chỉ định người thay Phụ trách bộ phận");
        var parents = employment.affectedShifts(id, at).stream()
                .map(s -> shifts.shift(s, false))
                .toList();
        var floors = new TreeSet<Long>();
        for (var shift : parents)
            if (shift.scopeId() != null) {
                if ("FLOOR".equals(shift.scopeType())) floors.add(shift.scopeId());
                else if ("AREA".equals(shift.scopeType()))
                    floors.addAll(sources.structure().floorsInArea(shift.scopeId()));
            }
        var managers = assignments.activeManagerAssignmentsForStaff(id, at).stream()
                .map(m -> assignments.manager(m, false))
                .toList();
        managers.forEach(m -> floors.add(m.floorAreaId()));
        assignments.lockFloors(floors);
        parents.stream().map(s -> s.scheduleId()).distinct().sorted().forEach(s -> shifts.schedule(s, true));
        parents.stream().map(s -> s.id()).sorted().forEach(s -> shifts.shift(s, true));
        var api = tasks.getIfAvailable();
        if ((person.accountId() != null || !parents.isEmpty()) && api == null) api = AssignmentSources.required(tasks);
        for (var shift : parents) {
            employment.withdraw(id, shift.id(), at);
            for (var a : assignments.inShift(shift.id(), true))
                if (a.staffId() == id
                        && !Set.of("ENDED", "CANCELLED").contains(a.status())
                        && a.endsAt().isAfter(at)) {
                    boolean cancelled =
                            "DRAFT".equals(a.status()) || a.startsAt().isAfter(at);
                    String next = cancelled ? "CANCELLED" : "ENDED";
                    assignments.transition(a, next, cancelled ? null : at, "Nhân viên nghỉ việc");
                    if (!cancelled) api.onAssignmentEnded(a.id(), a.shiftId());
                    audit.write(AuditLogEntry.builder()
                            .action(new AuditAction("ASSIGNMENT_ENDED_BY_EMPLOYMENT", false))
                            .entity("M09.ASSIGNMENT", a.id())
                            .before(Map.of("status", a.status()))
                            .after(Map.of("status", next))
                            .basis(new Basis("FR-004", null, id, MDC.get(SchedulerRuns.MDC_KEY)))
                            .build());
                    events.publishEvent(new AssignmentChanged(shift.id(), id, time.now()));
                }
            ratios.recalculate(shift.id(), "EMPLOYMENT_ENDED");
            events.publishEvent(new ShiftChanged(shift.id(), "EMPLOYMENT_ENDED", List.of(id), time.now(), List.of()));
            if (Objects.equals(shift.leadStaffId(), id)) {
                var affectedFloors = "FLOOR".equals(shift.scopeType())
                        ? List.of(shift.scopeId())
                        : "AREA".equals(shift.scopeType())
                                ? sources.structure().floorsInArea(shift.scopeId())
                                : List.<Long>of();
                events.publishEvent(new ShiftLeadMissing(
                        shift.id(),
                        time.now(),
                        notifications.missingLeadInFloors(shift.id(), affectedFloors, time.now())));
            }
        }
        for (var manager : managers)
            if ("ACTIVE".equals(manager.status())) {
                var end = manager.startsAt().isAfter(at) ? manager.startsAt() : at;
                assignments.shorten(manager, end, true, "Nhân viên nghỉ việc");
                audit.write(AuditLogEntry.builder()
                        .action(new AuditAction("FLOOR_MANAGER_ENDED_BY_EMPLOYMENT", false))
                        .entity("M09.FLOOR_MANAGER_ASSIGNMENT", manager.id())
                        .before(Map.of("status", manager.status(), "endsAt", String.valueOf(manager.endsAt())))
                        .after(Map.of("status", "ENDED", "endsAt", end))
                        .basis(new Basis("FR-004", null, id, MDC.get(SchedulerRuns.MDC_KEY)))
                        .build());
            }
        accounts.lockOnResignation(id, new Basis("FR-004", null, id, MDC.get(SchedulerRuns.MDC_KEY)));
        if (api != null) api.onResignation(id, at);
        employment.status(id, "RESIGNED", time.now());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("STAFF_EMPLOYMENT_ENDED", false))
                .entity("M09.STAFF", id)
                .before(Map.of("status", "WORKING"))
                .after(Map.of("status", "RESIGNED"))
                .basis(new Basis("FR-004", null, id, MDC.get(SchedulerRuns.MDC_KEY)))
                .details(Map.of(
                        "effectiveAt",
                        at,
                        "withdrawnShiftIds",
                        parents.stream().map(s -> s.id()).toList(),
                        "floorManagerAssignmentIds",
                        managers.stream().map(m -> m.id()).toList()))
                .build());
        events.publishEvent(
                new StaffEmploymentEnded(id, at, time.now(), notifications.employment(id, floors, time.now())));
    }
}
