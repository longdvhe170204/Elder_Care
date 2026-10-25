package com.cambae.backend.m09.shift.internal.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.*;
import com.cambae.backend.common.notification.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m09.api.events.SchedulePublished;
import com.cambae.backend.m09.credential.internal.domain.CredentialValidity;
import com.cambae.backend.m09.credential.internal.persistence.CredentialStore;
import com.cambae.backend.m09.shift.internal.domain.*;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m09.shift.internal.service.port.*;
import com.cambae.backend.m09.staff.internal.persistence.StaffStore;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import com.cambae.backend.m15.account.api.spi.*;
import com.cambae.backend.m15.auditlog.api.*;
import com.cambae.backend.m15.parameter.api.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T013: dữ liệu Nháp chưa có hiệu lực; công bố kiểm lại và lưu bản gốc trong cùng transaction. */
@Service
public class ShiftScheduleService {
    public record Mutation(long shiftId, List<PublicationWarning> warnings) {}

    public record InvalidAssignment(long assignmentId, String reason) {}

    public record Preview(
            List<UnmetCondition> unmetConditions,
            List<InvalidAssignment> invalidAssignments,
            List<PublicationWarning> warnings) {}

    private final ShiftStore store;
    private final StaffStore staff;
    private final CredentialStore credentials;
    private final AccountApi accounts;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> users;
    private final ObjectProvider<AssignmentApi> assignments;
    private final ObjectProvider<ExtendedPublicationCheck> extended;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final com.cambae.backend.m09.assignment.internal.service.AssignmentService staffAssignments;
    private final com.cambae.backend.m09.assignment.internal.service.AssignmentReminders assignmentReminders;
    private final com.cambae.backend.m09.ratio.internal.service.StaffingRatioService ratios;

    public ShiftScheduleService(
            ShiftStore store,
            StaffStore staff,
            CredentialStore credentials,
            AccountApi accounts,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> users,
            ObjectProvider<AssignmentApi> assignments,
            ObjectProvider<ExtendedPublicationCheck> extended,
            ParameterApi parameters,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            com.cambae.backend.m09.assignment.internal.service.AssignmentService staffAssignments,
            com.cambae.backend.m09.assignment.internal.service.AssignmentReminders assignmentReminders,
            com.cambae.backend.m09.ratio.internal.service.StaffingRatioService ratios) {
        this.store = store;
        this.staff = staff;
        this.credentials = credentials;
        this.accounts = accounts;
        this.permissions = permissions;
        this.users = users;
        this.assignments = assignments;
        this.extended = extended;
        this.parameters = parameters;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.staffAssignments = staffAssignments;
        this.assignmentReminders = assignmentReminders;
        this.ratios = ratios;
    }

    public List<ShiftTemplate> templates() {
        return store.templates();
    }

    public List<String> assignmentCommands(WorkShift shift) {
        return staffAssignments.allowedCommands(shift);
    }

    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.CREATE)
    @Transactional
    public ShiftTemplate createTemplate(TemplateInput input) {
        managerOnly();
        validateTemplate(input);
        long id = store.createTemplate(input, handover(input), time.now());
        var created = store.template(id, false);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("SHIFT_TEMPLATE_CREATED", false))
                .entity("M09.SHIFT_TEMPLATE", id)
                .after(Map.of("template", created))
                .build());
        return created;
    }

    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.UPDATE)
    @Transactional
    public ShiftTemplate updateTemplate(long id, String ifMatch, TemplateInput input) {
        managerOnly();
        validateTemplate(input);
        var before = store.template(id, true);
        VersionEtag.requireMatch(ifMatch, before.version(), before);
        store.updateTemplate(id, input, handover(input));
        var after = store.template(id, false);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("SHIFT_TEMPLATE_UPDATED", false))
                .entity("M09.SHIFT_TEMPLATE", id)
                .before(Map.of("template", before))
                .after(Map.of("template", after))
                .build());
        return after;
    }

    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.CREATE)
    @Transactional
    public ShiftSchedule createSchedule(YearMonth month, String scopeType, Long scopeId, Target target) {
        validateScope(scopeType, scopeId);
        if (month == null) throw invalid("Thiếu tháng của lịch");
        require(Operation.CREATE, target(scopeType, scopeId));
        try {
            return store.schedule(
                    store.createSchedule(
                            month, scopeType, scopeId, users.getObject().accountId(), time.now()),
                    false);
        } catch (DuplicateKeyException e) {
            throw conflict("SCHEDULE_ALREADY_EXISTS", "Đã có lịch của tháng và phạm vi này");
        }
    }

    @Transactional(readOnly = true)
    public List<ShiftSchedule> schedules(YearMonth month, Long scopeId) {
        if (!calendarReader()) throw new PermissionDeniedException("Nhân viên xem lịch riêng qua Ca của tôi");
        return store.schedules(month, scopeId).stream()
                .filter(s -> allowed(Operation.VIEW, target(s.scopeType(), s.scopeId())))
                .toList();
    }

    public Target targetOfSchedule(long id) {
        var s = store.schedule(id, false);
        return target(s.scopeType(), s.scopeId());
    }

    public Target targetOfShift(long id) {
        var s = store.shift(id, false);
        return target(s.scopeType(), s.scopeId());
    }

    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.UPDATE)
    @Transactional
    public Mutation addShift(long scheduleId, String ifMatch, ShiftInput input, Target target) {
        lockStaff(input.staff().stream().map(ShiftInput.Member::staffId).toList());
        var schedule = store.schedule(scheduleId, true);
        require(Operation.UPDATE, target(schedule.scopeType(), schedule.scopeId()));
        draft(schedule);
        VersionEtag.requireMatch(ifMatch, schedule.version(), schedule);
        var template = store.template(input.shiftTemplateId(), false);
        Instant[] interval = interval(schedule, template, input);
        var warnings = validateStaff(schedule, input, null, interval[0], interval[1]);
        confirm(warnings, input.confirmWarnings(), input.reason());
        long id = store.createShift(schedule, template, input, interval[0], interval[1]);
        var allWarnings = new ArrayList<>(warnings);
        allWarnings.addAll(ratios.recalculate(id, "DRAFT_SAVED"));
        return new Mutation(id, List.copyOf(allWarnings));
    }

    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.UPDATE)
    @Transactional
    public Mutation editShift(long id, String ifMatch, ShiftInput input, Target target) {
        var initial = store.shift(id, false);
        var ids = new ArrayList<>(
                store.members(id).stream().map(ShiftMember::staffId).toList());
        ids.addAll(input.staff().stream().map(ShiftInput.Member::staffId).toList());
        lockStaff(ids);
        var schedule = store.schedule(initial.scheduleId(), true);
        var before = store.shift(id, true);
        require(Operation.UPDATE, target(schedule.scopeType(), schedule.scopeId()));
        draft(schedule);
        VersionEtag.requireMatch(ifMatch, before.version(), before);
        var template = store.template(input.shiftTemplateId(), false);
        Instant[] interval = interval(schedule, template, input);
        var warnings = validateStaff(schedule, input, id, interval[0], interval[1]);
        confirm(warnings, input.confirmWarnings(), input.reason());
        staffAssignments.removeDraftMembers(
                id, input.staff().stream().map(ShiftInput.Member::staffId).toList());
        store.updateShift(before, template, input, interval[0], interval[1]);
        var allWarnings = new ArrayList<>(warnings);
        allWarnings.addAll(ratios.recalculate(id, "DRAFT_SAVED"));
        return new Mutation(id, List.copyOf(allWarnings));
    }

    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.UPDATE)
    @Transactional
    public void deleteShift(long id, String ifMatch, Target target) {
        var initial = store.shift(id, false);
        lockStaff(store.members(id).stream().map(ShiftMember::staffId).toList());
        var schedule = store.schedule(initial.scheduleId(), true);
        var shift = store.shift(id, true);
        require(Operation.UPDATE, target(schedule.scopeType(), schedule.scopeId()));
        draft(schedule);
        VersionEtag.requireMatch(ifMatch, shift.version(), shift);
        staffAssignments.removeDraftMembers(id, List.of());
        store.deleteShift(shift);
    }

    public WorkShift detail(long id) {
        var shift = store.shift(id, false);
        requireRead(shift);
        return shift;
    }

    public List<ShiftMember> members(long id) {
        detail(id);
        return store.members(id);
    }

    public List<WorkShift> scheduleShifts(long id) {
        require(Operation.VIEW, targetOfSchedule(id));
        return store.scheduleShifts(id);
    }

    public boolean canRead(WorkShift shift) {
        if (!permissions
                .permissions(users.getObject())
                .contains(new PermissionKey(Function.SHIFT_SCHEDULE, Operation.VIEW))) return false;
        if (calendarReader() && allowed(Operation.VIEW, target(shift.scopeType(), shift.scopeId()))) return true;
        return !"DRAFT".equals(shift.status())
                && users.getObject().staffId().stream()
                        .anyMatch(id -> store.members(shift.id()).stream().anyMatch(m -> m.staffId() == id));
    }

    public List<WorkShift> ownShifts(LocalDate from, LocalDate to) {
        if (!permissions
                .permissions(users.getObject())
                .contains(new PermissionKey(Function.SHIFT_SCHEDULE, Operation.VIEW)))
            throw new PermissionDeniedException("Bạn không có quyền xem lịch ca");
        var id = users.getObject().staffId();
        if (id.isEmpty()) return List.of();
        Instant begin = time.startOfDay(from), end = time.startOfDay(to);
        if (!begin.isBefore(end)) throw invalid("Ngày kết thúc phải sau ngày bắt đầu");
        return store.staffRange(id.get(), begin, end, false);
    }

    private boolean calendarReader() {
        return !Collections.disjoint(users.getObject().roles(), Set.of(Role.NURSING_HOME_MANAGER, Role.FLOOR_MANAGER));
    }

    private void requireRead(WorkShift shift) {
        if (!canRead(shift)) throw new NotFoundException();
    }

    @Transactional(readOnly = true)
    public Preview preview(long id) {
        if (!calendarReader()) throw new PermissionDeniedException("Chỉ người quản lý lịch xem trước công bố");
        var schedule = store.schedule(id, false);
        require(Operation.VIEW, targetOfSchedule(id));
        return preview(schedule);
    }

    private Preview preview(ShiftSchedule schedule) {
        var conditions = new ArrayList<UnmetCondition>();
        var warnings = new ArrayList<PublicationWarning>();
        var shifts = store.scheduleShifts(schedule.id());
        if (shifts.stream().anyMatch(s -> s.handoverRequired() && s.leadStaffId() == null))
            conditions.add(
                    condition("SHIFT_LEAD_REQUIRED", "Có ca yêu cầu bàn giao nhưng chưa có người phụ trách", "FR-015"));
        if (!"FACILITY".equals(schedule.scopeType()) && !floorCovered(schedule))
            conditions.add(condition(
                    "FLOOR_MANAGER_REQUIRED",
                    "Tầng/khu vực chưa có Trưởng tầng được giao cho toàn bộ tháng",
                    "FR-016"));
        Duration threshold = parameters.read(ParameterCode.CFG_M09_02).asDuration();
        for (var shift : shifts)
            for (var member : store.members(shift.id())) {
                var intervals = store.forStaff(member.staffId(), shift.id(), false).stream()
                        .map(s -> new ContinuousShiftChain.Interval(s.startsAt(), s.endsAt()))
                        .toList();
                Duration chain = ContinuousShiftChain.duration(
                        intervals, new ContinuousShiftChain.Interval(shift.startsAt(), shift.endsAt()));
                if (chain.compareTo(threshold) > 0)
                    warnings.add(warning(
                            "CONTINUOUS_WORK_HOURS",
                            "Chuỗi ca liên tục vượt ngưỡng cấu hình",
                            Map.of("staffId", member.staffId(), "shiftId", shift.id())));
                warnings.addAll(trainingWarnings(member.staffId(), shift.startsAt(), shift.endsAt()));
            }
        extended.orderedStream().forEach(check -> warnings.addAll(check.check(schedule.id())));
        for (var shift : shifts) warnings.addAll(ratios.preview(shift.id()));
        var invalid = staffAssignments.invalidDrafts(schedule.id()).stream()
                .map(a -> new InvalidAssignment(a.assignmentId(), a.reason()))
                .toList();
        return new Preview(List.copyOf(conditions), invalid, List.copyOf(new LinkedHashSet<>(warnings)));
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.APPROVE)
    @Transactional
    public void publish(
            long id, List<Long> assignmentsToRemove, boolean confirmWarnings, Reason reason, Target target) {
        var staffIds = store.scheduleShifts(id).stream()
                .flatMap(s -> store.members(s.id()).stream())
                .map(ShiftMember::staffId)
                .distinct()
                .sorted()
                .toList();
        lockStaff(staffIds);
        staffAssignments.lockScheduleScope(id);
        var schedule = store.schedule(id, true);
        require(Operation.APPROVE, target(schedule.scopeType(), schedule.scopeId()));
        draft(schedule);
        var shifts = store.scheduleShifts(id);
        var actual = shifts.stream()
                .flatMap(s -> store.members(s.id()).stream())
                .map(ShiftMember::staffId)
                .distinct()
                .sorted()
                .toList();
        if (!staffIds.equals(actual))
            throw conflict("STATE_CHANGED", "Danh sách nhân viên đã thay đổi, hãy xem lại lịch");
        staffAssignments.removeDrafts(id, assignmentsToRemove);
        var check = preview(schedule);
        if (!check.invalidAssignments().isEmpty())
            throw conflict(
                    "INVALID_ASSIGNMENTS",
                    "Có phân công Nháp không hợp lệ; hãy gỡ các dòng đã liệt kê và kiểm tra lại");
        if (!check.unmetConditions().isEmpty()) throw new ConditionNotMetException(check.unmetConditions());
        boolean self = Objects.equals(schedule.createdBy(), users.getObject().accountId());
        if (self && reason == null) throw new ReasonRequiredException();
        // FR-036: cảnh báo tỷ lệ không chặn công bố và không phát sinh yêu cầu lý do riêng.
        confirm(
                check.warnings().stream()
                        .filter(w -> !Set.of("BELOW_STAFFING_THRESHOLD", "STAFFING_RATIO_UNAVAILABLE")
                                .contains(w.code()))
                        .toList(),
                confirmWarnings,
                reason);
        for (var shift : shifts) {
            var input = new ShiftInput(
                    shift.shiftTemplateId(),
                    LocalDate.ofInstant(shift.startsAt(), BusinessTime.ZONE),
                    shift.startsAt(),
                    shift.endsAt(),
                    store.members(shift.id()).stream()
                            .map(m -> new ShiftInput.Member(m.staffId(), m.onDutyDoctor()))
                            .toList(),
                    shift.leadStaffId(),
                    true,
                    reason);
            validateStaff(schedule, input, shift.id(), shift.startsAt(), shift.endsAt());
        }
        Instant now = time.now();
        store.publish(
                schedule, shifts, users.getObject().accountId(), self, reason == null ? null : reason.value(), now);
        staffAssignments.activateSchedule(id);
        for (var shift : shifts) ratios.recalculate(shift.id(), "PUBLISHED");
        store.savePublication(id, now, staffAssignments.snapshot(id));
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("SCHEDULE_PUBLISHED", self))
                .entity("M09.SCHEDULE", id)
                .before(Map.of("status", "DRAFT"))
                .after(Map.of("status", "PUBLISHED"))
                .reason(reason)
                .details(Map.of("selfApproved", self, "permissionBasis", "ROLE"))
                .build());
        List<NotificationRequest> requests = new ArrayList<>();
        for (long staffId : staffIds) {
            String key = "SCHEDULE_PUBLISHED:" + id + ":" + now + ":" + staffId;
            var own = shifts.stream()
                    .filter(s -> store.members(s.id()).stream().anyMatch(m -> m.staffId() == staffId))
                    .map(WorkShift::id)
                    .toList();
            requests.add(new NotificationRequest(
                    "008",
                    "SCHEDULE_PUBLISHED",
                    SourceGroup.SCHEDULE_CHANGE,
                    new SourceRecord("M09.SCHEDULE", Long.toString(id)),
                    key,
                    Optional.of(NotificationLevel.LOW),
                    Optional.empty(),
                    Optional.ofNullable(schedule.scopeId()),
                    List.of(new RecipientGroup.Staff(staffId, "SCHEDULE_PUBLISHED")),
                    List.of(new ContentPart(InformationType.GENERAL, "Lịch ca của bạn đã được công bố")),
                    Optional.of("Lịch ca của bạn đã được công bố"),
                    Optional.of("Lịch ca"),
                    Optional.of(users.getObject().accountId()),
                    true,
                    now,
                    Map.of("month", schedule.month().toString(), "shiftIds", own),
                    false,
                    Optional.empty(),
                    Optional.empty()));
        }
        assignmentReminders.published(id, now);
        events.publishEvent(new SchedulePublished(id, staffIds, now, requests));
    }

    List<PublicationWarning> validateStaff(
            ShiftSchedule schedule, ShiftInput input, Long exclude, Instant from, Instant to) {
        var warnings = new ArrayList<PublicationWarning>();
        var ids = new HashSet<Long>();
        Duration threshold = parameters.read(ParameterCode.CFG_M09_02).asDuration();
        for (var member : input.staff()) {
            if (!ids.add(member.staffId())) throw invalid("Nhân viên bị lặp trong ca");
            var profile = staff.find(member.staffId()).orElseThrow(NotFoundException::new);
            var account = accounts.byStaff(member.staffId())
                    .orElseThrow(() -> invalid("Nhân viên chưa có tài khoản liên kết"));
            if (!"WORKING".equals(profile.status()) || account.status() == AccountStatus.LOCKED)
                throw invalid("Nhân viên không còn đủ điều kiện xếp ca");
            if (member.onDutyDoctor()
                    && (!"FACILITY".equals(schedule.scopeType())
                            || !account.roles().contains(Role.DOCTOR)))
                throw invalid("Bác sĩ trực phải có vai trò Bác sĩ trong ca toàn viện");
            var existing = store.forStaff(member.staffId(), exclude, true);
            if (existing.stream()
                    .anyMatch(s -> s.startsAt().isBefore(to) && s.endsAt().isAfter(from)))
                throw conflict("SHIFT_OVERLAP", "Nhân viên đã có ca chồng giờ");
            Duration chain = ContinuousShiftChain.duration(
                    existing.stream()
                            .map(s -> new ContinuousShiftChain.Interval(s.startsAt(), s.endsAt()))
                            .toList(),
                    new ContinuousShiftChain.Interval(from, to));
            if (chain.compareTo(threshold) > 0)
                warnings.add(warning(
                        "CONTINUOUS_WORK_HOURS",
                        "Chuỗi ca liên tục vượt ngưỡng cấu hình",
                        Map.of("staffId", member.staffId())));
            warnings.addAll(trainingWarnings(member.staffId(), from, to));
        }
        if (input.leadStaffId() != null) {
            var account = accounts.byStaff(input.leadStaffId())
                    .orElseThrow(() -> invalid("Người phụ trách chưa có tài khoản"));
            if (!ids.contains(input.leadStaffId())
                    || Collections.disjoint(account.roles(), Set.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF)))
                throw invalid("Người phụ trách phải có tên trong ca và vai trò Trưởng tầng hoặc Điều dưỡng");
        }
        return List.copyOf(warnings);
    }

    private List<PublicationWarning> trainingWarnings(long staffId, Instant from, Instant to) {
        var roles = accounts.byStaff(staffId).orElseThrow().roles();
        var warnings = new ArrayList<PublicationWarning>();
        for (var type : credentials.types()) {
            if (!type.active()
                    || !"TRAINING".equals(type.group())
                    || roles.stream().map(Enum::name).noneMatch(type.requiredForRoles()::contains)) continue;
            boolean valid = credentials.staffCredentials(staffId).stream()
                    .filter(c -> c.credentialTypeId() == type.id())
                    .anyMatch(c -> CredentialValidity.covers(
                            c.issueDate(),
                            c.expiryDate(),
                            c.revokedFrom(),
                            c.replaced(),
                            from,
                            to.minusNanos(1),
                            time));
            if (!valid)
                warnings.add(warning(
                        "REQUIRED_TRAINING_EXPIRED",
                        "Đào tạo bắt buộc chưa còn hiệu lực cho toàn bộ ca",
                        Map.of("staffId", staffId, "credentialTypeId", type.id())));
        }
        return warnings;
    }

    private boolean floorCovered(ShiftSchedule schedule) {
        var api = assignments.getIfAvailable();
        if (api == null) return false;
        Instant from = time.startOfDay(schedule.month().atDay(1)),
                to = time.startOfDay(schedule.month().plusMonths(1).atDay(1));
        var ranges = staff.all().stream()
                .flatMap(s -> api.history(s.id(), from, to).stream())
                .filter(a ->
                        a.kind() == AssignmentKind.FLOOR_MANAGER && Objects.equals(a.floorAreaId(), schedule.scopeId()))
                .sorted(Comparator.comparing(AssignmentView::from))
                .toList();
        Instant cursor = from;
        for (var range : ranges) {
            if (range.from().isAfter(cursor)) return false;
            Instant end = range.to() == null ? to : range.to();
            if (end.isAfter(cursor)) cursor = end;
            if (!cursor.isBefore(to)) return true;
        }
        return false;
    }

    private Instant[] interval(ShiftSchedule schedule, ShiftTemplate template, ShiftInput input) {
        if (!template.active()
                || !template.scopeType().equals(schedule.scopeType())
                || input.date() == null
                || !YearMonth.from(input.date()).equals(schedule.month()))
            throw invalid("Mẫu ca hoặc ngày không phù hợp lịch");
        Instant from = input.startsAt() == null
                ? input.date()
                        .atTime(template.startTime())
                        .atZone(BusinessTime.ZONE)
                        .toInstant()
                : input.startsAt();
        Instant to = input.endsAt() == null
                ? input.date()
                        .plusDays(!template.endTime().isAfter(template.startTime()) ? 1 : 0)
                        .atTime(template.endTime())
                        .atZone(BusinessTime.ZONE)
                        .toInstant()
                : input.endsAt();
        Duration duration = Duration.between(from, to);
        if (duration.isZero()
                || duration.isNegative()
                || duration.compareTo(Duration.ofDays(1)) > 0
                || !LocalDate.ofInstant(from, BusinessTime.ZONE).equals(input.date()))
            throw invalid("Ca phải bắt đầu đúng ngày và dài hơn 0, không quá 24 giờ");
        return new Instant[] {from, to};
    }

    private void lockStaff(Collection<Long> ids) {
        ids.stream().distinct().sorted().forEach(staff::lock);
    }

    private boolean allowed(Operation operation, Target target) {
        return permissions
                .hasPermission(users.getObject(), new PermissionKey(Function.SHIFT_SCHEDULE, operation), target)
                .allowed();
    }

    private void require(Operation operation, Target target) {
        if (!allowed(operation, target))
            throw new PermissionDeniedException("Bạn không có quyền trên lịch của phạm vi này");
        if (target.floorAreaId() == null && !users.getObject().roles().contains(Role.NURSING_HOME_MANAGER))
            throw new PermissionDeniedException("Lịch toàn viện chỉ do Quản lý viện quản lý");
    }

    private static Target target(String scope, Long id) {
        return new Target("M09.SCHEDULE", null, null, "FACILITY".equals(scope) ? null : id);
    }

    private void managerOnly() {
        if (!users.getObject().roles().contains(Role.NURSING_HOME_MANAGER))
            throw new PermissionDeniedException("Chỉ Quản lý viện cấu hình mẫu ca");
    }

    private static void draft(ShiftSchedule schedule) {
        if (!"DRAFT".equals(schedule.status())) throw conflict("STATE_CHANGED", "Lịch đã công bố, không sửa trực tiếp");
    }

    private static void validateScope(String type, Long id) {
        if (type == null
                || !Set.of("FLOOR", "AREA", "FACILITY").contains(type)
                || ("FACILITY".equals(type) ? id != null : id == null || id <= 0))
            throw invalid("Phạm vi lịch không hợp lệ");
    }

    private static void validateTemplate(TemplateInput input) {
        if (input.name() == null
                || input.name().isBlank()
                || input.startTime() == null
                || input.endTime() == null
                || input.startTime().getSecond() != 0
                || input.endTime().getSecond() != 0) throw invalid("Mẫu ca thiếu tên hoặc giờ không hợp lệ");
        validateScope(input.scopeType(), "FACILITY".equals(input.scopeType()) ? null : 1L);
    }

    private static boolean handover(TemplateInput input) {
        return input.handoverRequired() != null ? input.handoverRequired() : !"FACILITY".equals(input.scopeType());
    }

    static void confirm(List<PublicationWarning> warnings, boolean confirmed, Reason reason) {
        if (warnings.isEmpty()) return;
        if (!confirmed)
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "WARNING_CONFIRMATION_REQUIRED",
                    "Cần xác nhận đã xem cảnh báo",
                    Map.of("warnings", warnings));
        if (reason == null) throw new ReasonRequiredException();
    }

    private static PublicationWarning warning(String code, String description, Map<String, Object> details) {
        return new PublicationWarning(code, description, details);
    }

    private static UnmetCondition condition(String code, String description, String basis) {
        return new UnmetCondition(code, description, basis, "008", false);
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", message);
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
