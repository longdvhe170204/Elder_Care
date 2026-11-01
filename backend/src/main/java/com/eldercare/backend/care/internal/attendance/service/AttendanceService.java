package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.api.spi.DayCarePickupPort;
import com.eldercare.backend.care.internal.attendance.domain.*;
import com.eldercare.backend.care.internal.attendance.persistence.AttendanceStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.CareTaskTransitions;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class AttendanceService {
    private final AttendanceStore store;
    private final AttendanceSources sources;
    private final CareLockStore locks;
    private final CurrentUser user;
    private final PermissionApi permissions;
    private final BusinessTime time;
    private final ObjectProvider<DayCarePickupPort> pickups;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final CareTaskStore tasks;
    private final CareTaskService taskCommands;

    public AttendanceService(
            AttendanceStore store,
            AttendanceSources sources,
            CareLockStore locks,
            CurrentUser user,
            PermissionApi permissions,
            BusinessTime time,
            ObjectProvider<DayCarePickupPort> pickups,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            CareTaskStore tasks,
            CareTaskService taskCommands) {
        this.store = store;
        this.sources = sources;
        this.locks = locks;
        this.user = user;
        this.permissions = permissions;
        this.time = time;
        this.pickups = pickups;
        this.audit = audit;
        this.events = events;
        this.tasks = tasks;
        this.taskCommands = taskCommands;
    }

    public Target target(long resident, long floor) {
        return new Target("M04.DAY_CARE_ATTENDANCE", "0", resident, floor);
    }

    public Target target(long id) {
        var row = store.require(id);
        return new Target(
                "M04.DAY_CARE_ATTENDANCE",
                Long.toString(id),
                row.residentId(),
                row.content().floorAreaId());
    }

    public void permit(CurrentUser actor, Operation op, Target target) {
        if (!permissions
                .hasPermission(actor, new PermissionKey(Function.DAY_CARE_ATTENDANCE, op), target)
                .allowed()) throw new PermissionDeniedException("Không có quyền điểm danh với đối tượng này");
    }

    private void recorder() {
        if (Collections.disjoint(user.roles(), Set.of(Role.OFFICE_STAFF, Role.CAREGIVER, Role.NURSING_STAFF)))
            throw new PermissionDeniedException("Chỉ Hành chính, Nhân viên chăm sóc hoặc Điều dưỡng được điểm danh");
    }

    public static BusinessException conflict() {
        return new BusinessException(
                HttpStatus.CONFLICT, "ATTENDANCE_STATE_CONFLICT", "Không thể điểm danh ở trạng thái hiện tại");
    }

    private AttendanceData initial(long floor, AttendanceDaySource source) {
        return new AttendanceData(
                floor,
                source.arrival(),
                source.departure(),
                source.departure(),
                null,
                null,
                null,
                null,
                null,
                null,
                !source.scheduled(),
                source.calendar().timelyAbsenceId(),
                source.calendar().lateAbsenceId());
    }

    @RequiresPermission(function = Function.DAY_CARE_ATTENDANCE, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    public SimpleRecord<AttendanceData> checkIn(Target target, long resident, Instant at) {
        recorder();
        var date = at.atZone(BusinessTime.ZONE).toLocalDate();
        locks.lock(List.of(resident));
        sources.checkResident(resident, date);
        var source = sources.day(resident, date);
        var before = store.find(resident, date).map(r -> store.lock(r.id())).orElse(null);
        long floor = before == null ? sources.floor() : before.content().floorAreaId();
        permit(user, Operation.CREATE, target(resident, floor));
        if (before != null
                && AttendanceTransitions.next(AttendanceStatus.valueOf(before.status()), "ARRIVE")
                        .isEmpty()) throw conflict();
        if (source.calendar().timelyAbsenceId() != null && !source.calendar().holiday()) throw conflict();
        var row = before == null
                ? store.create(resident, date, AttendanceStatus.PRESENT, initial(floor, source), user.accountId())
                : before;
        var record = store.add(row, "ARRIVE", at, null, null, user.accountId(), time.now());
        var data = row.content()
                .calendar(
                        source.arrival(),
                        source.departure(),
                        source.calendar().timelyAbsenceId(),
                        source.calendar().lateAbsenceId())
                .arrive(record.id(), at, !source.scheduled());
        var after = store.change(row, AttendanceStatus.PRESENT, data);
        log(before, after, "CHECKED_IN", at);
        var context = context(
                after,
                "ARRIVED:" + record.id(),
                at,
                List.of(new RecipientGroup.ShiftLead(floor, new Substitution.Default())));
        events.publishEvent(new DayCareCheckedIn(
                resident, date, at.atZone(BusinessTime.ZONE).toLocalTime(), context));
        events.publishEvent(new TaskPlanningNeeded(resident, "DAY_CARE_ARRIVAL", at, context));
        return after;
    }

    @RequiresPermission(function = Function.DAY_CARE_ATTENDANCE, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    public AttendanceCheckoutResult checkOut(Target target, long id, long pickup, Instant at) {
        recorder();
        var first = store.require(id);
        locks.lock(List.of(first.residentId()));
        var before = store.lock(id);
        permit(user, Operation.CREATE, target(id));
        if (!before.status().equals("PRESENT")
                || before.content().arrivedAt() == null
                || at.isBefore(before.content().arrivedAt())) throw conflict();
        var p = pickups.getIfAvailable();
        if (p == null) throw AttendanceSources.unavailable("Chưa có nguồn bản ghi đón012");
        var valid = p.valid(pickup, before.residentId(), at)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT, "INVALID_PICKUP_RECORD", "Bản ghi đón không hợp lệ hoặc đã dùng"));
        CheckoutType type;
        try {
            type = CheckoutType.valueOf(valid.pickupType());
        } catch (IllegalArgumentException e) {
            throw AttendanceSources.unavailable("Nguồn012 trả loại đón chưa được công bố");
        }
        var record = store.add(before, "LEAVE", at, type, pickup, user.accountId(), time.now());
        p.markUsed(pickup, record.id());
        var after = store.change(before, AttendanceStatus.LEFT, before.content().leave(record.id(), at, type));
        log(before, after, "CHECKED_OUT", at);
        return new AttendanceCheckoutResult(after, cancelFuture(before.residentId(), at));
    }

    private List<Long> cancelFuture(long resident, Instant from) {
        var cancelled = new ArrayList<Long>();
        for (var candidate : tasks.notYetDueFrom(resident, from)) {
            var before = tasks.get(candidate.id(), true);
            if (before.status() != TaskStatus.NOT_YET_DUE) continue;
            var current = CareTaskTransitions.initialStatus(before.windowFrom(), before.windowTo(), time.now());
            if (current != TaskStatus.NOT_YET_DUE) {
                var after = tasks.transition(before, current, time.now(), null, null);
                taskCommands.record(before, after, "TIME_CHANGED", null, true);
                continue;
            }
            var reason = new Reason("Người bán trú đã về");
            var after =
                    tasks.transition(before, TaskStatus.CANCELLED, time.now(), reason.value(), "FR_019_DAY_CARE_LEFT");
            taskCommands.record(before, after, "DAY_CARE_LEFT", reason, true);
            cancelled.add(before.id());
        }
        return List.copyOf(cancelled);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void refresh(long resident, LocalDate date) {
        locks.lock(List.of(resident));
        sources.checkResident(resident, date);
        var source = sources.day(resident, date);
        var before = store.find(resident, date).map(r -> store.lock(r.id())).orElse(null);
        if (before == null) {
            if (!source.scheduled()) return;
            var status = source.calendar().timelyAbsenceId() == null
                    ? AttendanceStatus.NOT_ARRIVED
                    : AttendanceStatus.REPORTED_ABSENT;
            var row = store.create(resident, date, status, initial(sources.floor(), source), user.accountId());
            log(null, row, "CALENDAR_DETERMINED", time.now());
            return;
        }
        var status = AttendanceStatus.valueOf(before.status());
        if (source.calendar().holiday())
            status = AttendanceTransitions.next(status, "HOLIDAY").orElse(status);
        else if (source.calendar().timelyAbsenceId() != null)
            status = AttendanceTransitions.next(status, "REPORT_ABSENCE").orElse(status);
        boolean keepAttendedTimes =
                before.content().arrivedAt() != null && source.calendar().holiday();
        var data = before.content()
                .calendar(
                        keepAttendedTimes ? before.content().expectedArrival() : source.arrival(),
                        keepAttendedTimes ? before.content().expectedReturn() : source.departure(),
                        source.calendar().timelyAbsenceId(),
                        source.calendar().lateAbsenceId());
        if (before.status().equals(status.name()) && before.content().equals(data)) return;
        var after = store.change(before, status, data);
        log(before, after, "CALENDAR_CHANGED", time.now());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void returnTime(long resident, LocalDate date, LocalTime newTime, long registration, boolean restore) {
        locks.lock(List.of(resident));
        var found = store.find(resident, date);
        if (found.isEmpty()) {
            refresh(resident, date);
            found = store.find(resident, date);
        }
        var row = store.lock(found.orElseThrow(NotFoundException::new).id());
        if (restore && !Objects.equals(row.content().returnRegistrationId(), registration)) return;
        var data = row.content()
                .returnTime(
                        restore ? row.content().expectedReturn() : Objects.requireNonNull(newTime),
                        restore ? null : registration);
        if (data.equals(row.content())) return;
        log(
                row,
                store.change(row, AttendanceStatus.valueOf(row.status()), data),
                restore ? "RETURN_TIME_RESTORED" : "RETURN_TIME_CHANGED",
                time.now());
    }

    public CareEventContext context(
            SimpleRecord<AttendanceData> row, String milestone, Instant at, List<RecipientGroup> recipients) {
        return new CareEventContext(
                "M04.DAY_CARE_ATTENDANCE",
                row.id(),
                milestone,
                at,
                Optional.of(row.content().floorAreaId()),
                recipients);
    }

    public void log(
            SimpleRecord<AttendanceData> before, SimpleRecord<AttendanceData> after, String command, Instant at) {
        var oldValues = before == null ? Map.<String, Object>of() : snapshot(before);
        var newValues = snapshot(after);
        var changedBefore = new LinkedHashMap<String, Object>();
        var changedAfter = new LinkedHashMap<String, Object>();
        for (var entry : newValues.entrySet()) {
            if (before == null || !Objects.equals(oldValues.get(entry.getKey()), entry.getValue())) {
                if (before != null) changedBefore.put(entry.getKey(), oldValues.get(entry.getKey()));
                changedAfter.put(entry.getKey(), entry.getValue());
            }
        }
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("DAY_CARE_" + command, false))
                .entity("M04.DAY_CARE_ATTENDANCE", after.id())
                .resident(after.residentId())
                .before(changedBefore)
                .after(changedAfter)
                .build());
        var context = context(after, command + ":" + after.version(), at, List.of());
        if (before == null)
            events.publishEvent(new AttendanceDetermined(
                    after.residentId(),
                    after.effectiveFrom(),
                    AttendanceStatus.valueOf(after.status()),
                    after.content().adHocSession(),
                    context));
        else
            events.publishEvent(new AttendanceChanged(
                    after.residentId(),
                    after.effectiveFrom(),
                    AttendanceStatus.valueOf(after.status()),
                    after.content().adHocSession(),
                    context));
    }

    private Map<String, Object> snapshot(SimpleRecord<AttendanceData> row) {
        var d = row.content();
        var values = new LinkedHashMap<String, Object>();
        values.put("status", row.status());
        values.put("expectedArrival", d.expectedArrival());
        values.put("expectedReturn", d.expectedReturn());
        values.put("currentExpectedReturn", d.currentExpectedReturn());
        values.put("arrivedAt", d.arrivedAt());
        values.put("leftAt", d.leftAt());
        values.put("checkoutType", d.checkoutType());
        values.put("adHocSession", d.adHocSession());
        values.put("arrivalRecordId", d.arrivalRecordId());
        values.put("departureRecordId", d.departureRecordId());
        values.put("returnRegistrationId", d.returnRegistrationId());
        values.put("timelyAbsenceId", d.timelyAbsenceId());
        values.put("lateAbsenceId", d.lateAbsenceId());
        return values;
    }
}
