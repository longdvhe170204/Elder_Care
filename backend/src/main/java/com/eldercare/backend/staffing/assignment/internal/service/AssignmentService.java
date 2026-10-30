package com.eldercare.backend.staffing.assignment.internal.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.events.AssignmentChanged;
import com.eldercare.backend.staffing.assignment.internal.domain.*;
import com.eldercare.backend.staffing.assignment.internal.persistence.AssignmentStore;
import com.eldercare.backend.staffing.credential.internal.domain.CredentialValidity;
import com.eldercare.backend.staffing.credential.internal.persistence.CredentialStore;
import com.eldercare.backend.staffing.shift.internal.domain.*;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.staffing.shift.internal.service.port.PublicationWarning;
import com.eldercare.backend.staffing.staff.internal.persistence.StaffStore;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-025–032: cùng phép kiểm cho xem trước, tạo và công bố; nguồn thiếu thì từ chối. */
@Service
public class AssignmentService {
    public record Result(Long id, List<PublicationWarning> warnings) {}

    public record NotCopied(long sourceAssignmentId, String reason) {}

    public record CopyResult(List<Long> copiedIds, List<NotCopied> notCopied) {}

    public record InvalidDraft(long assignmentId, String reason) {}

    private final AssignmentStore store;
    private final ShiftStore shifts;
    private final StaffStore staff;
    private final CredentialStore credentials;
    private final AssignmentSources sources;
    private final AccountApi accounts;
    private final ProfessionalActivityApi activities;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> users;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;

    public AssignmentService(
            AssignmentStore store,
            ShiftStore shifts,
            StaffStore staff,
            CredentialStore credentials,
            AssignmentSources sources,
            AccountApi accounts,
            ProfessionalActivityApi activities,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> users,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events) {
        this.store = store;
        this.shifts = shifts;
        this.staff = staff;
        this.credentials = credentials;
        this.sources = sources;
        this.accounts = accounts;
        this.activities = activities;
        this.permissions = permissions;
        this.users = users;
        this.time = time;
        this.audit = audit;
        this.events = events;
    }

    public Target target(long id) {
        var a = store.find(id, false);
        return targetOfShift(a.shiftId());
    }

    public Target targetOfShift(long id) {
        var s = shifts.shift(id, false);
        return new Target("M09.SHIFT", Long.toString(id), null, s.scopeId());
    }

    public List<StaffAssignment> list(long shift) {
        var s = shifts.shift(shift, false);
        boolean manager = allowed(Operation.VIEW, s)
                && !Collections.disjoint(
                        users.getObject().roles(), Set.of(Role.NURSING_HOME_MANAGER, Role.FLOOR_MANAGER));
        boolean own = !Set.of("DRAFT", "CANCELLED").contains(s.status())
                && users.getObject()
                        .staffId()
                        .map(id -> shifts.members(shift).stream().anyMatch(m -> m.staffId() == id))
                        .orElse(false);
        if (!manager && !own) throw new NotFoundException();
        return store.inShift(shift, false);
    }

    public List<String> allowedCommands(WorkShift s) {
        var result = new ArrayList<String>();
        if (!Set.of("DRAFT", "PUBLISHED", "IN_PROGRESS").contains(s.status())) return List.of();
        if (allowed(Operation.CREATE, s)) {
            result.add("createAssignment");
            result.add("copyAssignments");
        }
        if (allowed(Operation.UPDATE, s)) {
            if ("DRAFT".equals(s.status())) result.add("deleteAssignment");
            else {
                result.add("cancelAssignment");
                result.add("endAssignment");
            }
        } else if (isCurrentLead(s)) result.add("endAssignment");
        return List.copyOf(result);
    }

    private boolean isCurrentLead(WorkShift s) {
        return !s.missingLead()
                && Objects.equals(s.leadStaffId(), users.getObject().staffId().orElse(null))
                && !time.now().isBefore(s.startsAt())
                && time.now().isBefore(s.endsAt())
                && shifts.members(s.id()).stream()
                        .anyMatch(m -> Objects.equals(m.staffId(), s.leadStaffId()) && m.presentAt(time.now()));
    }

    @Transactional(readOnly = true)
    public Result preview(long shift, AssignmentInput input, Target target) {
        var s = shifts.shift(shift, false);
        require(Operation.CREATE, s);
        var row = row(s, input, "MANUAL");
        return new Result(null, validate(row, s, null, false));
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.CREATE)
    @Transactional
    public Result create(long shift, AssignmentInput input, Target target) {
        staff.lock(input.staffId());
        var initial = shifts.shift(shift, false);
        var row = row(initial, input, "MANUAL");
        lockScope(initial, row);
        shifts.schedule(initial.scheduleId(), true);
        var s = shifts.shift(shift, true);
        require(Operation.CREATE, s);
        row = row(s, input, "MANUAL");
        var warnings = validate(row, s, null, true);
        return save(row, warnings);
    }

    private Result save(StaffAssignment row, List<PublicationWarning> warnings) {
        return save(row, warnings, null);
    }

    private Result save(StaffAssignment row, List<PublicationWarning> warnings, Basis basis) {
        long id = store.insert(row, users.getObject().accountId(), time.now());
        if (!"DRAFT".equals(row.status())) {
            if (basis == null)
                log(
                        "ASSIGNMENT_CREATED",
                        id,
                        Map.of(),
                        Map.of("status", row.status(), "staffId", row.staffId(), "shiftId", row.shiftId()),
                        null);
            else
                audit.write(AuditLogEntry.builder()
                        .action(new AuditAction("ASSIGNMENT_CREATED", false))
                        .entity("M09.ASSIGNMENT", id)
                        .after(Map.of("status", row.status(), "staffId", row.staffId(), "shiftId", row.shiftId()))
                        .basis(basis)
                        .details(Map.of("shiftId", row.shiftId(), "source", "SHIFT_SWAP_015"))
                        .build());
            changed(row);
        }
        return new Result(id, warnings);
    }

    List<UnmetCondition> validateFromRequest(long shift, AssignmentInput input) {
        try {
            var s = shifts.shift(shift, false);
            validate(row(s, input, "SHIFT_SWAP_015"), s, null, false);
            return List.of();
        } catch (BusinessException e) {
            return List.of(new UnmetCondition(e.errorCode(), e.getMessage()));
        }
    }

    long createFromRequest(long shift, AssignmentInput input, Basis basis) {
        Objects.requireNonNull(basis);
        staff.lock(input.staffId());
        var initial = shifts.shift(shift, false);
        var candidate = row(initial, input, "SHIFT_SWAP_015");
        lockScope(initial, candidate);
        shifts.schedule(initial.scheduleId(), true);
        var s = shifts.shift(shift, true);
        candidate = row(s, input, "SHIFT_SWAP_015");
        return save(candidate, validate(candidate, s, null, true), basis).id();
    }

    void cancelFromRequest(long id, Basis basis) {
        Objects.requireNonNull(basis);
        var a = store.find(id, false);
        staff.lock(a.staffId());
        var initial = shifts.shift(a.shiftId(), false);
        lockScope(initial, a);
        shifts.schedule(initial.scheduleId(), true);
        shifts.shift(initial.id(), true);
        a = store.find(id, true);
        if (!"SCHEDULED".equals(a.status())) throw invalid("Chỉ hủy phân công tương lai trong lệnh đổi ca");
        store.transition(a, "CANCELLED", null, "Điều chỉnh theo yêu cầu đổi ca");
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("ASSIGNMENT_CANCELLED", false))
                .entity("M09.ASSIGNMENT", id)
                .before(Map.of("status", a.status()))
                .after(Map.of("status", "CANCELLED"))
                .basis(basis)
                .details(Map.of("shiftId", a.shiftId()))
                .build());
        events.publishEvent(new AssignmentChanged(a.shiftId(), a.staffId(), time.now()));
    }

    public StaffAssignment row(WorkShift shift, AssignmentInput input, String source) {
        Instant from = input.startsAt() == null ? shift.startsAt() : input.startsAt(),
                to = input.endsAt() == null ? shift.endsAt() : input.endsAt();
        String status = "DRAFT".equals(shift.status()) ? "DRAFT" : from.isAfter(time.now()) ? "SCHEDULED" : "ACTIVE";
        return new StaffAssignment(
                0,
                shift.id(),
                input.staffId(),
                input.targetType(),
                input.targetId(),
                input.taskTypeId(),
                input.role(),
                input.assignmentKind(),
                input.assignedNurse(),
                from,
                to,
                status,
                null,
                source,
                0);
    }

    private List<PublicationWarning> validate(StaffAssignment row, WorkShift s, Long exclude, boolean lock) {
        if (!Set.of("DRAFT", "PUBLISHED", "IN_PROGRESS").contains(s.status()))
            throw invalid("Ca không còn cho phép lập phân công");
        if (row.role() == null
                || !Set.of("PRIMARY", "SUPPORT").contains(row.assignmentKind())
                || !row.endsAt().isAfter(row.startsAt())
                || row.startsAt().isBefore(s.startsAt())
                || row.endsAt().isAfter(s.endsAt())) throw invalid("Khoảng giờ hoặc vai trò phân công không hợp lệ");
        var member = shifts.members(s.id(), lock).stream()
                .filter(m -> m.staffId() == row.staffId())
                .findFirst()
                .orElseThrow(() -> invalid("Nhân viên không có tên trong ca"));
        if (!member.presentAt(row.startsAt()) || !member.presentAt(row.endsAt().minusNanos(1)))
            throw invalid("Nhân viên vắng hoặc không còn ở ca trong khoảng phân công");
        var profile = staff.find(row.staffId()).orElseThrow(NotFoundException::new);
        var account = accounts.byStaff(row.staffId()).orElseThrow(() -> invalid("Nhân viên chưa có tài khoản"));
        if (!"WORKING".equals(profile.status())
                || Set.of(AccountStatus.LOCKED, AccountStatus.INACTIVE).contains(account.status())
                || !canDo(account.roles(), row.role()))
            throw invalid("Nhân viên không đủ vai trò hoặc không còn đang làm việc");
        sources.validateTarget(row, s);
        var required = new HashSet<String>();
        if (row.taskTypeId() != null) {
            var task = sources.requirements(row.taskTypeId());
            if (task.role() != null && !canDo(Set.of(row.role()), task.role()))
                throw invalid("Vai trò phân công không đáp ứng loại công việc");
            required.addAll(task.certificates());
        }
        if (row.assignedNurse()) {
            if (row.role() != Role.NURSING_STAFF || !account.roles().contains(Role.NURSING_STAFF))
                throw invalid("Điều dưỡng phụ trách phải có vai trò Điều dưỡng");
            required.add(activities
                    .requiredLicenseType(ProfessionalActivityApi.MEDICATION_ADMINISTRATION)
                    .orElseThrow(() -> invalid("Chưa cấu hình loại giấy phép cho phát thuốc")));
        }
        if (!qualified(row.staffId(), required, row.startsAt(), row.endsAt()))
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "ASSIGNMENT_CREDENTIAL_REQUIRED",
                    "Thiếu giấy phép hoặc đào tạo còn hiệu lực cho toàn bộ khoảng phân công");
        // Khi xem trước chỉ đọc; khi ghi, khóa tầng đã giữ và phép đọc hiện hành chặn lệnh đồng thời.
        if ("PRIMARY".equals(row.assignmentKind())
                && !store.duplicateCandidates(row, exclude, lock).isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "PRIMARY_ASSIGNMENT_OVERLAP",
                    "Đã có nhân viên chính trùng đối tượng, vai trò, loại công việc và thời gian");
        var warnings = new ArrayList<PublicationWarning>();
        if ("PRIMARY".equals(row.assignmentKind()) && row.taskTypeId() == null) {
            var missed = new TreeSet<Long>();
            for (long resident : sources.residents(row.targetType(), row.targetId(), row.startsAt()))
                for (long taskId : sources.taskTypes(resident)) {
                    var task = sources.requirements(taskId);
                    if (task.role() != null && !canDo(Set.of(row.role()), task.role())
                            || !qualified(row.staffId(), task.certificates(), row.startsAt(), row.endsAt()))
                        missed.add(taskId);
                }
            if (!missed.isEmpty())
                warnings.add(new PublicationWarning(
                        "UNQUALIFIED_TASK_TYPES",
                        "Một số loại công việc sẽ thành công việc chung vì nhân viên không đủ điều kiện",
                        Map.of("taskTypeIds", List.copyOf(missed), "staffId", row.staffId())));
        }
        return List.copyOf(warnings);
    }

    public boolean qualified(long person, Set<String> required, Instant from, Instant to) {
        var all = credentials.staffCredentials(person);
        return required.stream()
                .allMatch(type -> all.stream()
                        .filter(c -> c.typeCode().equals(type)
                                || Long.toString(c.credentialTypeId()).equals(type))
                        .anyMatch(c -> CredentialValidity.covers(
                                c.issueDate(),
                                c.expiryDate(),
                                c.revokedFrom(),
                                c.replaced(),
                                from,
                                to.minusNanos(1),
                                time)));
    }

    public static boolean canDo(Set<Role> roles, Role required) {
        return roles.contains(required) || required == Role.CAREGIVER && roles.contains(Role.NURSING_STAFF);
    }

    private Set<Long> scopeLocks(WorkShift s, StaffAssignment a) {
        var floors = new TreeSet<>(sources.floors(a.targetType(), a.targetId(), a.startsAt()));
        if (s.scopeId() != null) floors.add(s.scopeId());
        return floors;
    }

    public void lockScope(WorkShift s, StaffAssignment a) {
        store.lockFloors(scopeLocks(s, a));
    }

    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.UPDATE)
    @Transactional
    public void delete(long id, Target target) {
        var initial = store.find(id, false);
        staff.lock(initial.staffId());
        var s = shifts.shift(initial.shiftId(), false);
        lockScope(s, initial);
        shifts.schedule(s.scheduleId(), true);
        s = shifts.shift(s.id(), true);
        var a = store.find(id, true);
        require(Operation.UPDATE, s);
        if (!"DRAFT".equals(a.status()) || !"DRAFT".equals(s.status()))
            throw invalid("Chỉ xóa phân công Nháp trong ca Nháp");
        store.delete(id);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.UPDATE)
    @Transactional
    public void cancel(long id, Reason reason, Target target) {
        finish(id, reason, false);
    }
    // FR-027: quyền xem là điều kiện nền, quyền ghi kiểm thêm đúng ca/người phụ trách, không cấp UPDATE cho mọi Điều
    // dưỡng.
    @BusinessCommand
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.VIEW)
    @Transactional
    public void end(long id, Reason reason, Target target) {
        finish(id, reason, true);
    }

    private void finish(long id, Reason reason, boolean early) {
        var initial = store.find(id, false);
        staff.lock(initial.staffId());
        var s = shifts.shift(initial.shiftId(), false);
        lockScope(s, initial);
        shifts.schedule(s.scheduleId(), true);
        s = shifts.shift(s.id(), true);
        var a = store.find(id, true);
        var lockedShift = s;
        boolean lead = early
                && Objects.equals(s.leadStaffId(), users.getObject().staffId().orElse(null))
                && !s.missingLead()
                && !time.now().isBefore(s.startsAt())
                && time.now().isBefore(s.endsAt())
                && shifts.members(s.id()).stream()
                        .anyMatch(m -> m.staffId() == lockedShift.leadStaffId() && m.presentAt(time.now()));
        if (!lead) require(Operation.UPDATE, s);
        if (!"ACTIVE".equals(early ? a.status() : "ACTIVE") || !early && !"SCHEDULED".equals(a.status()))
            throw invalid("Trạng thái phân công đã thay đổi");
        Instant now = time.now();
        if (early && (now.isBefore(a.startsAt()) || !now.isBefore(a.endsAt())))
            throw invalid("Phân công không còn trong khoảng hiệu lực");
        String status = early ? "ENDED" : "CANCELLED";
        store.transition(a, status, early ? now : null, reason.value());
        if (early) sources.ended(a.id(), s.id());
        log(
                early ? "ASSIGNMENT_ENDED" : "ASSIGNMENT_CANCELLED",
                id,
                Map.of("status", a.status()),
                Map.of("status", status),
                reason);
        events.publishEvent(new AssignmentChanged(s.id(), a.staffId(), now));
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.CREATE)
    @Transactional
    public CopyResult copy(long source, long targetShift, Target target) {
        var original = shifts.shift(source, false);
        var destination = shifts.shift(targetShift, false);
        require(Operation.CREATE, destination);
        if (source == targetShift
                || !original.scopeType().equals(destination.scopeType())
                || !Objects.equals(original.scopeId(), destination.scopeId()))
            throw invalid("Ca nguồn và ca đích phải khác nhau, cùng phạm vi");
        var rows = store.inShift(source, false);
        rows.stream().map(StaffAssignment::staffId).distinct().sorted().forEach(staff::lock);
        var copyFloors = new TreeSet<Long>();
        for (var a : rows)
            copyFloors.addAll(scopeLocks(
                    destination,
                    new StaffAssignment(
                            0,
                            targetShift,
                            a.staffId(),
                            a.targetType(),
                            a.targetId(),
                            a.taskTypeId(),
                            a.role(),
                            a.assignmentKind(),
                            a.assignedNurse(),
                            destination.startsAt(),
                            destination.endsAt(),
                            "DRAFT",
                            null,
                            "COPIED",
                            0)));
        store.lockFloors(copyFloors);
        java.util.stream.LongStream.of(original.scheduleId(), destination.scheduleId())
                .distinct()
                .sorted()
                .forEach(id -> shifts.schedule(id, true));
        java.util.stream.LongStream.of(source, targetShift).sorted().forEach(id -> shifts.shift(id, true));
        destination = shifts.shift(targetShift, true);
        if (!rows.equals(store.inShift(source, true)))
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "STATE_CHANGED",
                    "Phân công nguồn đã thay đổi; hãy sao chép lại sau khi tải lại ca");
        var copied = new ArrayList<Long>();
        var skipped = new ArrayList<NotCopied>();
        for (var a : rows) {
            if (Set.of("CANCELLED").contains(a.status())) {
                skipped.add(new NotCopied(a.id(), "Phân công nguồn đã hủy"));
                continue;
            }
            var input = new AssignmentInput(
                    a.staffId(),
                    a.targetType(),
                    a.targetId(),
                    a.taskTypeId(),
                    a.role(),
                    a.assignmentKind(),
                    a.assignedNurse(),
                    null,
                    null);
            var candidate = row(destination, input, "COPIED");
            List<PublicationWarning> warnings;
            try {
                warnings = validate(candidate, destination, null, true);
            } catch (BusinessException ex) {
                skipped.add(new NotCopied(a.id(), ex.getMessage()));
                continue;
            }
            copied.add(save(candidate, warnings).id());
        }
        return new CopyResult(List.copyOf(copied), List.copyOf(skipped));
    }

    public List<InvalidDraft> invalidDrafts(long schedule) {
        var result = new ArrayList<InvalidDraft>();
        for (var a : store.inSchedule(schedule)) {
            if (!"DRAFT".equals(a.status())) continue;
            try {
                validate(a, shifts.shift(a.shiftId(), false), a.id(), false);
            } catch (BusinessException ex) {
                result.add(new InvalidDraft(a.id(), ex.getMessage()));
            }
        }
        return result;
    }

    public void lockScheduleScope(long schedule) {
        var parent = shifts.schedule(schedule, false);
        var floors = new TreeSet<Long>();
        if (parent.scopeId() != null) floors.add(parent.scopeId());
        for (var a : store.inSchedule(schedule)) floors.addAll(scopeLocks(shifts.shift(a.shiftId(), false), a));
        store.lockFloors(floors);
    }

    public void removeDrafts(long schedule, List<Long> ids) {
        var rows = store.inSchedule(schedule);
        for (long id : ids) {
            var a = rows.stream()
                    .filter(r -> r.id() == id && "DRAFT".equals(r.status()))
                    .findFirst()
                    .orElseThrow(() -> invalid("Không tìm thấy phân công Nháp trong lịch"));
            store.delete(a.id());
        }
    }

    public void activateSchedule(long schedule) {
        var rows = store.inSchedule(schedule).stream()
                .filter(a -> "DRAFT".equals(a.status()))
                .toList();
        for (var a : rows) validate(a, shifts.shift(a.shiftId(), false), a.id(), true);
        for (var a : rows) {
            String status = a.startsAt().isAfter(time.now())
                    ? "SCHEDULED"
                    : a.endsAt().isAfter(time.now()) ? "ACTIVE" : "ENDED";
            store.transition(a, status, null, null);
        }
        for (var a : rows) if (a.endsAt().isAfter(time.now())) changed(a);
    }

    public List<StaffAssignment> snapshot(long schedule) {
        return store.inSchedule(schedule);
    }

    public void removeDraftMembers(long shift, Collection<Long> remaining) {
        store.removeDrafts(shift, remaining);
    }

    private void changed(StaffAssignment row) {
        for (long resident : sources.residents(row.targetType(), row.targetId(), row.startsAt()))
            sources.changed(resident, row.shiftId());
        events.publishEvent(new AssignmentChanged(row.shiftId(), row.staffId(), time.now()));
    }

    private boolean allowed(Operation op, WorkShift s) {
        return permissions
                .hasPermission(
                        users.getObject(),
                        new PermissionKey(Function.SHIFT_SCHEDULE, op),
                        new Target("M09.SHIFT", Long.toString(s.id()), null, s.scopeId()))
                .allowed();
    }

    private void require(Operation op, WorkShift s) {
        if (!allowed(op, s)) throw new PermissionDeniedException("Bạn không có quyền trên phân công của ca này");
        if (s.scopeId() == null && !users.getObject().roles().contains(Role.NURSING_HOME_MANAGER))
            throw new PermissionDeniedException("Chỉ Quản lý viện lập phân công toàn viện");
    }

    private void log(String action, long id, Map<String, Object> before, Map<String, Object> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, false))
                .entity("M09.ASSIGNMENT", id)
                .before(before)
                .after(after)
                .reason(reason)
                .details(Map.of("basis", "FR-027"))
                .build());
    }

    private static BusinessException invalid(String detail) {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_ASSIGNMENT", detail);
    }
}
