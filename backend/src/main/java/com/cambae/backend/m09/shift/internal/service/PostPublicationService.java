package com.cambae.backend.m09.shift.internal.service;

import com.cambae.backend.common.*;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m04.api.ShiftTaskApi;
import com.cambae.backend.m09.api.events.*;
import com.cambae.backend.m09.assignment.internal.persistence.AssignmentStore;
import com.cambae.backend.m09.assignment.internal.service.AssignmentSources;
import com.cambae.backend.m09.ratio.internal.service.StaffingRatioService;
import com.cambae.backend.m09.shift.internal.domain.*;
import com.cambae.backend.m09.shift.internal.persistence.*;
import com.cambae.backend.m09.shift.internal.service.port.*;
import com.cambae.backend.m09.staff.internal.persistence.StaffStore;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import com.cambae.backend.m15.auditlog.api.*;
import com.cambae.backend.m15.parameter.api.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T040: khóa nhân viên → phạm vi → lịch → ca; mọi tác động và nhật ký cùng transaction. */
@Service
public class PostPublicationService {
    private final ShiftStore shifts;
    private final PostPublicationStore store;
    private final StaffStore staff;
    private final AssignmentStore assignments;
    private final AssignmentSources sources;
    private final ShiftScheduleService schedules;
    private final AccountApi accounts;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> users;
    private final ObjectProvider<ShiftTaskApi> tasks;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final StaffingRatioService ratios;
    private final ApplicationEventPublisher events;
    private final PostPublicationNotifications notices;

    public PostPublicationService(
            ShiftStore shifts,
            PostPublicationStore store,
            StaffStore staff,
            AssignmentStore assignments,
            AssignmentSources sources,
            ShiftScheduleService schedules,
            AccountApi accounts,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> users,
            ObjectProvider<ShiftTaskApi> tasks,
            BusinessTime time,
            ParameterApi parameters,
            AuditLogApi audit,
            StaffingRatioService ratios,
            ApplicationEventPublisher events,
            PostPublicationNotifications notices) {
        this.shifts = shifts;
        this.store = store;
        this.staff = staff;
        this.assignments = assignments;
        this.sources = sources;
        this.schedules = schedules;
        this.accounts = accounts;
        this.permissions = permissions;
        this.users = users;
        this.tasks = tasks;
        this.time = time;
        this.parameters = parameters;
        this.audit = audit;
        this.ratios = ratios;
        this.events = events;
        this.notices = notices;
    }

    public Target target(long id) {
        var s = shifts.shift(id, false);
        return new Target("M09.SHIFT", Long.toString(id), null, s.scopeId());
    }

    private WorkShift lock(long id, Collection<Long> extra) {
        var s = shifts.shift(id, false);
        var people = new TreeSet<Long>(extra);
        shifts.members(id).forEach(m -> people.add(m.staffId()));
        people.forEach(staff::lock);
        var floors = new TreeSet<Long>();
        if (s.scopeId() != null) {
            floors.add(s.scopeId());
            if ("AREA".equals(s.scopeType())) floors.addAll(sources.structure().floorsInArea(s.scopeId()));
        }
        assignments.lockFloors(floors);
        shifts.schedule(s.scheduleId(), true);
        return shifts.shift(id, true);
    }

    private boolean manages(WorkShift s) {
        var u = users.getObject();
        return u.roles().contains(Role.NURSING_HOME_MANAGER)
                || u.roles().contains(Role.FLOOR_MANAGER)
                        && s.scopeId() != null
                        && permissions
                                .hasPermission(
                                        u, new PermissionKey(Function.SHIFT_SCHEDULE, Operation.UPDATE), target(s.id()))
                                .allowed();
    }

    private boolean lead(WorkShift s) {
        return !s.missingLead()
                && Objects.equals(s.leadStaffId(), users.getObject().staffId().orElse(null))
                && shifts.members(s.id()).stream()
                        .anyMatch(m -> Objects.equals(m.staffId(), s.leadStaffId()) && m.presentAt(time.now()));
    }

    private static void require(boolean allowed) {
        if (!allowed) throw new PermissionDeniedException("Bạn không có quyền thực hiện lệnh trên ca này");
    }

    private void open(WorkShift s) {
        if (!Set.of("PUBLISHED", "IN_PROGRESS").contains(s.status()))
            throw conflict("STATE_CHANGED", "Ca không còn ở trạng thái cho phép điều chỉnh");
    }

    private void ongoing(WorkShift s) {
        open(s);
        if (!time.now().isBefore(s.endsAt())) throw conflict("STATE_CHANGED", "Ca đã hết giờ");
    }

    private ShiftMember present(WorkShift s, long id) {
        return shifts.members(s.id(), true).stream()
                .filter(m -> m.staffId() == id && m.presentAt(time.now()))
                .findFirst()
                .orElseThrow(
                        () -> conflict("STAFF_NOT_IN_SHIFT", "Nhân viên không còn có tên trong ca tại thời điểm này"));
    }

    private boolean eligible(WorkShift s, long id) {
        return shifts.members(s.id(), true).stream().anyMatch(m -> m.staffId() == id && m.presentAt(time.now()))
                && staff.find(id).filter(p -> "WORKING".equals(p.status())).isPresent()
                && accounts.byStaff(id)
                        .filter(a -> a.status() != AccountStatus.LOCKED
                                && !Collections.disjoint(a.roles(), Set.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF)))
                        .isPresent();
    }

    public List<String> allowedCommands(WorkShift s) {
        var result = new ArrayList<String>();
        if (users.getObject().roles().contains(Role.NURSING_HOME_MANAGER)) result.add("viewDutyBasisAudit");
        if (!Set.of("PUBLISHED", "IN_PROGRESS").contains(s.status())) return List.copyOf(result);
        var now = time.now();
        if (manages(s) && now.isBefore(s.endsAt())) {
            result.add("addStaff");
            result.add("changeLead");
        }
        if ((manages(s) || lead(s))
                && !now.isBefore(s.startsAt()
                        .minus(parameters.read(ParameterCode.CFG_M15_07).asDuration()))
                && !now.isAfter(s.endsAt())) result.add("recordAbsence");
        if (users.getObject().roles().contains(Role.NURSING_HOME_MANAGER)
                && "PUBLISHED".equals(s.status())
                && now.isBefore(s.startsAt())) result.add("cancelShift");
        return List.copyOf(result);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.UPDATE)
    @Transactional
    public List<PublicationWarning> addStaff(long id, long person, boolean confirmed, Reason reason, Target target) {
        var s = lock(id, List.of(person));
        require(manages(s));
        ongoing(s);
        return add(s, person, confirmed, reason, "ADDED", null);
    }

    private List<PublicationWarning> add(
            WorkShift s, long person, boolean confirmed, Reason reason, String entry, Basis basis) {
        long id = s.id();
        if (shifts.members(id, true).stream().anyMatch(m -> m.staffId() == person))
            throw conflict("STAFF_ALREADY_IN_SHIFT", "Nhân viên đã có trong lịch sử ca; không ghi đè dòng cũ");
        var start = time.now().isAfter(s.startsAt()) ? time.now() : s.startsAt();
        var input = new ShiftInput(
                s.shiftTemplateId(),
                time.today(),
                start,
                s.endsAt(),
                List.of(new ShiftInput.Member(person, false)),
                null,
                confirmed,
                reason);
        var warnings = schedules.validateStaff(shifts.schedule(s.scheduleId(), false), input, id, start, s.endsAt());
        ShiftScheduleService.confirm(warnings, confirmed, reason);
        store.add(id, person, time.now(), entry, reason == null ? null : reason.value());
        log(
                "STAFF_ADDED_TO_SHIFT",
                "M09.SHIFT",
                id,
                s,
                reason,
                Map.of("staffId", person, "entryBasis", entry),
                "INVITED".equals(entry) ? "ROLE_INVITATION_SENT" : "ROLE",
                basis);
        var all = new ArrayList<>(warnings);
        all.addAll(ratios.recalculate(id, basis == null ? "STAFF_ADDED" : "SHIFT_SWAP_015"));
        events.publishEvent(new ShiftChanged(
                id,
                "STAFF_ADDED",
                List.of(person),
                time.now(),
                notices.request("STAFF_ADDED", s, List.of(person), time.now(), false)));
        return List.copyOf(all);
    }

    List<PublicationWarning> addFromRequest(long id, long person, ShiftEntryBasis basis, boolean confirmed) {
        Objects.requireNonNull(basis);
        var s = lock(id, List.of(person));
        ongoing(s);
        return add(s, person, confirmed, basis.reason(), basis.kind().name(), basis.auditBasis());
    }

    void leaveFromRequest(long id, long person, ShiftDepartureBasis basis) {
        Objects.requireNonNull(basis);
        var s = lock(id, List.of(person));
        ongoing(s);
        present(s, person);
        store.leave(
                id,
                person,
                time.now(),
                basis.kind().name(),
                basis.kind() == ShiftDepartureBasis.Kind.APPROVED_LEAVE ? basis.sourceId() : null);
        endAssignments(s, person, time.now(), new Reason("Điều chỉnh ca theo yêu cầu đã duyệt"), false);
        log(
                "STAFF_LEFT_SHIFT",
                "M09.SHIFT_STAFF",
                store.memberId(id, person),
                s,
                null,
                Map.of("staffId", person, "departureBasis", basis.kind().name()),
                "ROLE",
                basis.auditBasis());
        ratios.recalculate(id, "SHIFT_SWAP_015");
        events.publishEvent(new ShiftChanged(
                id,
                "SHIFT_SWAP_015",
                List.of(person),
                time.now(),
                notices.request("SHIFT_SWAP_015", s, List.of(person), time.now(), false)));
    }

    void leadFromRequest(long id, long person, Basis basis) {
        Objects.requireNonNull(basis);
        var s = lock(id, List.of(person));
        ongoing(s);
        if (!eligible(s, person)) throw conflict("INVALID_SHIFT_LEAD", "Người phụ trách mới không đủ điều kiện");
        replace(s, person, "SHIFT_SWAP_015", null);
        log("SHIFT_LEAD_CHANGED", "M09.SHIFT", id, s, null, Map.of("newStaffId", person), "ROLE", basis);
    }

    void missingFromRequest(long id, Basis basis) {
        Objects.requireNonNull(basis);
        var s = lock(id, List.of());
        ongoing(s);
        if (shifts.members(id, true).stream().anyMatch(m -> eligible(s, m.staffId())))
            throw conflict("REPLACEMENT_LEAD_REQUIRED", "Vẫn còn người đủ điều kiện phụ trách ca");
        replace(s, null, "SHIFT_SWAP_015", null);
        log("SHIFT_LEAD_MISSING", "M09.SHIFT", id, s, null, Map.of(), "ROLE", basis);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.UPDATE)
    @Transactional
    public void changeLead(long id, long person, Reason reason, Target target) {
        var s = lock(id, List.of(person));
        require(manages(s));
        ongoing(s);
        if (Objects.equals(s.leadStaffId(), person))
            throw conflict("STATE_CHANGED", "Nhân viên này đã là Người phụ trách ca");
        if (!eligible(s, person))
            throw conflict(
                    "INVALID_SHIFT_LEAD",
                    "Người phụ trách mới phải có mặt trong ca, vai trò Trưởng tầng hoặc Điều dưỡng và tài khoản chưa Đã khóa");
        replace(s, person, "CHANGED", reason);
        log("SHIFT_LEAD_CHANGED", "M09.SHIFT", id, s, reason, Map.of("newStaffId", person), "ROLE");
    }

    private void replace(WorkShift s, Long person, String basis, Reason reason) {
        store.changeLead(
                s.id(),
                person,
                time.now(),
                basis,
                users.getObject().accountId(),
                reason == null ? null : reason.value());
        if (person != null)
            store.reassignDraft(s.id(), person).ifPresent(h -> {
                events.publishEvent(
                        new HandoverReassigned(h, person, time.now(), notices.handover(s, h, person, time.now())));
            });
        if (person == null)
            events.publishEvent(new ShiftLeadMissing(
                    s.id(), time.now(), notices.request("LEAD_MISSING", s, List.of(), time.now(), true)));
        var affected = new ArrayList<Long>();
        if (s.leadStaffId() != null) affected.add(s.leadStaffId());
        if (person != null) affected.add(person);
        events.publishEvent(new ShiftChanged(
                s.id(),
                person == null ? "LEAD_MISSING" : "LEAD_CHANGED",
                affected,
                time.now(),
                person == null ? List.of() : notices.request("LEAD_CHANGED", s, affected, time.now(), false)));
    }

    @BusinessCommand
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.VIEW)
    @Transactional
    public void recordAbsence(long id, long person, Long replacement, Reason reason, Target target) {
        var extra = new ArrayList<Long>();
        extra.add(person);
        if (replacement != null) extra.add(replacement);
        var s = lock(id, extra);
        require(manages(s) || lead(s));
        open(s);
        var now = time.now();
        if (now.isBefore(s.startsAt()
                        .minus(parameters.read(ParameterCode.CFG_M15_07).asDuration()))
                || now.isAfter(s.endsAt()))
            throw conflict(
                    "OUTSIDE_ABSENCE_REPORTING_WINDOW",
                    "Ngoài khoảng ghi nhận vắng ca; hãy lập yêu cầu nghỉ đột xuất ở mục Đổi ca, nghỉ phép");
        present(s, person);
        Long next = null;
        if (Objects.equals(s.leadStaffId(), person)) {
            var choices = shifts.members(id, true).stream()
                    .filter(m -> m.staffId() != person && eligible(s, m.staffId()))
                    .map(ShiftMember::staffId)
                    .toList();
            if (replacement != null) {
                if (!choices.contains(replacement))
                    throw conflict("INVALID_SHIFT_LEAD", "Người thay không đủ điều kiện phụ trách ca");
                next = replacement;
            } else if (!choices.isEmpty())
                throw conflict("REPLACEMENT_LEAD_REQUIRED", "Cần chọn Người phụ trách thay thế trong cùng lệnh");
        } else if (replacement != null)
            throw conflict("INVALID_SHIFT_LEAD", "Chỉ chọn người phụ trách thay khi ghi vắng Người phụ trách hiện tại");
        store.absent(id, person, now, reason.value(), users.getObject().accountId());
        var effectReason = new Reason("Kết thúc nhiệm vụ do nhân viên vắng ca");
        endAssignments(s, person, now, effectReason, false);
        AssignmentSources.required(tasks).onAbsence(person, id);
        if (Objects.equals(s.leadStaffId(), person)) replace(s, next, "ABSENCE", effectReason);
        ratios.recalculate(id, "STAFF_ABSENT");
        log(
                "STAFF_ABSENCE_RECORDED",
                "M09.STAFF_ABSENCE",
                store.memberId(id, person),
                s,
                reason,
                Map.of("staffId", person),
                manages(s) ? "ROLE" : "SHIFT_LEAD");
        events.publishEvent(
                new StaffAbsent(id, person, now, notices.request("STAFF_ABSENT", s, List.of(person), now, false)));
    }

    private void endAssignments(WorkShift s, Long person, Instant at, Reason reason, boolean cancelAll) {
        for (var a : assignments.inShift(s.id(), true)) {
            if (person != null && a.staffId() != person
                    || Set.of("ENDED", "CANCELLED").contains(a.status())) continue;
            if (!cancelAll && !a.endsAt().isAfter(at)) continue;
            boolean cancel =
                    cancelAll || "DRAFT".equals(a.status()) || a.startsAt().isAfter(at);
            assignments.transition(
                    a, cancel ? "CANCELLED" : "ENDED", cancel ? null : at, reason == null ? null : reason.value());
            if (!cancel) sources.ended(a.id(), s.id());
            log(
                    cancel ? "ASSIGNMENT_CANCELLED" : "ASSIGNMENT_ENDED",
                    "M09.ASSIGNMENT",
                    a.id(),
                    s,
                    reason,
                    Map.of("staffId", a.staffId()),
                    manages(s) ? "ROLE" : "SHIFT_LEAD");
            events.publishEvent(new AssignmentChanged(s.id(), a.staffId(), at));
        }
    }

    @BusinessCommand
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.UPDATE)
    @Transactional
    public void cancel(long id, Reason reason, Target target) {
        var s = lock(id, List.of());
        require(users.getObject().roles().contains(Role.NURSING_HOME_MANAGER));
        if (!"PUBLISHED".equals(s.status()) || !time.now().isBefore(s.startsAt()))
            throw conflict("STATE_CHANGED", "Chỉ hủy ca đã công bố chưa bắt đầu");
        endAssignments(s, null, time.now(), reason, true);
        store.cancel(id, reason.value(), time.now());
        log("SHIFT_CANCELLED", "M09.SHIFT", id, s, reason, Map.of(), "ROLE");
        var members = shifts.members(id).stream().map(ShiftMember::staffId).toList();
        events.publishEvent(new ShiftChanged(
                id, "CANCELLED", members, time.now(), notices.request("CANCELLED", s, members, time.now(), false)));
    }

    private void log(
            String action,
            String type,
            long id,
            WorkShift s,
            Reason reason,
            Map<String, Object> details,
            String basis) {
        log(action, type, id, s, reason, details, basis, null);
    }

    private void log(
            String action,
            String type,
            long id,
            WorkShift s,
            Reason reason,
            Map<String, Object> details,
            String basis,
            Basis source) {
        var data = new LinkedHashMap<String, Object>(details);
        data.put("shiftId", s.id());
        data.put("basis", basis);
        var entry = AuditLogEntry.builder()
                .action(new AuditAction(action, false))
                .entity(type, id)
                .reason(reason)
                .basis(source)
                .details(data);
        // Bộ che nhật ký xét các trường thay đổi: khai báo reason để STAFF_REASON được áp cả cho lý do nhật ký.
        if ("M09.STAFF_ABSENCE".equals(type))
            entry.before(Map.of("absent", false)).after(Map.of("absent", true, "reason", reason.value()));
        audit.write(entry.build());
    }

    private static BusinessException conflict(String code, String text) {
        return new BusinessException(HttpStatus.CONFLICT, code, text);
    }
}
