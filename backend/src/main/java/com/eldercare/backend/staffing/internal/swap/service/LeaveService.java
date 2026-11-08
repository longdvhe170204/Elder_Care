package com.eldercare.backend.staffing.internal.swap.service;

import static com.eldercare.backend.staffing.internal.swap.service.LeaveHandler.TYPE;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.CreateRequest;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.care.api.ShiftTaskApi;
import com.eldercare.backend.care.api.TripDutyCommandApi;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.events.SchedulePublished;
import com.eldercare.backend.staffing.api.spi.TripDutyPort;
import com.eldercare.backend.staffing.internal.assignment.domain.StaffAssignment;
import com.eldercare.backend.staffing.internal.assignment.persistence.AssignmentStore;
import com.eldercare.backend.staffing.internal.assignment.service.AssignmentService;
import com.eldercare.backend.staffing.internal.shift.domain.ShiftInput;
import com.eldercare.backend.staffing.internal.shift.domain.ShiftMember;
import com.eldercare.backend.staffing.internal.shift.domain.ShiftSchedule;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.shift.service.ShiftCommandPort;
import com.eldercare.backend.staffing.internal.shift.service.ShiftDepartureBasis;
import com.eldercare.backend.staffing.internal.staff.persistence.StaffStore;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveDay;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveDetail;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveDetail.Item;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveDetail.LeaveKind;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveDetail.Result;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveInput;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveView;
import com.eldercare.backend.staffing.internal.swap.domain.SwapStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Nghỉ đột xuất (015 FR-026 → FR-031): theo ca hoặc theo ngày, lập thay, duyệt từng mục, tác động khi duyệt qua port lệnh
 * nội bộ của 008 và API của 005, 014 trong một transaction (research X-09). Chi tiết ở {@code simple_record}
 * {@code M09.LEAVE_DETAIL}; nghỉ theo ngày đã duyệt ở {@code M09.LEAVE_DAY}.
 */
@Service
public class LeaveService implements ApprovedLeaveDayPort {

    private final ShiftStore shifts;
    private final StaffStore staff;
    private final AssignmentStore assignments;
    private final AssignmentService assignmentCommands;
    private final AccountApi accounts;
    private final ParameterApi parameters;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> users;
    private final BusinessTime time;
    private final ApprovalRequestApi approvals;
    private final SwapRecords records;
    private final ShiftCommandPort shiftPort;
    private final ObjectProvider<ShiftTaskApi> tasks;
    private final ObjectProvider<TripDutyCommandApi> tripCommands;
    private final ObjectProvider<TripDutyPort> trips;
    private final AuditLogApi audit;
    private final SwapNotices notices;
    private final JsonMapper json;

    public LeaveService(
            ShiftStore shifts,
            StaffStore staff,
            AssignmentStore assignments,
            AssignmentService assignmentCommands,
            AccountApi accounts,
            ParameterApi parameters,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> users,
            BusinessTime time,
            ApprovalRequestApi approvals,
            SwapRecords records,
            ShiftCommandPort shiftPort,
            ObjectProvider<ShiftTaskApi> tasks,
            ObjectProvider<TripDutyCommandApi> tripCommands,
            ObjectProvider<TripDutyPort> trips,
            AuditLogApi audit,
            SwapNotices notices,
            JsonMapper json) {
        this.shifts = shifts;
        this.staff = staff;
        this.assignments = assignments;
        this.assignmentCommands = assignmentCommands;
        this.accounts = accounts;
        this.parameters = parameters;
        this.permissions = permissions;
        this.users = users;
        this.time = time;
        this.approvals = approvals;
        this.records = records;
        this.shiftPort = shiftPort;
        this.tasks = tasks;
        this.tripCommands = tripCommands;
        this.trips = trips;
        this.audit = audit;
        this.notices = notices;
        this.json = json;
    }

    /** Quyết định của người duyệt cho một mục (ca hoặc ngày). */
    public record ItemDecision(Long shiftId, LocalDate date, boolean approved, Long newLeadId) {}

    public record Decision(List<ItemDecision> items, String reason) {}

    // ---- Lập ---------------------------------------------------------------------------------------------------

    /** FR-026, FR-026a: một yêu cầu cho mỗi phạm vi; trả mã các yêu cầu đã tạo. */
    @Transactional
    public List<Long> create(LeaveInput input) {
        CurrentUser me = users.getObject();
        long actorAccount = me.accountId();
        long myStaff = me.staffId().orElseThrow(() -> new PermissionDeniedException("Chỉ nhân viên lập yêu cầu nghỉ"));
        long target = input.staffId() == null ? myStaff : input.staffId();
        boolean onBehalf = target != myStaff;
        var profile = staff.find(target).orElseThrow(NotFoundException::new);
        if (!"WORKING".equals(profile.status())) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STAFF", "Nhân viên không còn làm việc");
        }
        staff.lock(target);
        List<Long> created = new ArrayList<>();
        if (input.kind() == LeaveKind.BY_SHIFT) {
            Map<String, List<WorkShift>> groups = new LinkedHashMap<>();
            for (WorkShift s : collectShifts(input, target)) {
                requireLeavable(s, target);
                groups.computeIfAbsent(s.scopeType() + ":" + s.scopeId(), k -> new ArrayList<>())
                        .add(shifts.shift(s.id(), true));
            }
            if (groups.isEmpty()) {
                throw new BusinessException(HttpStatus.CONFLICT, "INVALID_SHIFT", "Không có ca nào để xin nghỉ");
            }
            for (List<WorkShift> group : groups.values()) {
                WorkShift first = group.get(0);
                if (onBehalf) {
                    requireApprover(me, first.scopeType(), first.scopeId());
                }
                List<Item> items = group.stream()
                        .sorted(Comparator.comparing(WorkShift::startsAt))
                        .map(s -> new Item(s.id(), null, Result.PENDING, null, null, null))
                        .toList();
                created.add(file(input, target, onBehalf, actorAccount, first.scopeType(), first.scopeId(), items));
            }
        } else {
            if (input.dates().isEmpty() || input.scopeType() == null) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Nghỉ theo ngày cần ngày và phạm vi");
            }
            if (onBehalf) {
                requireApprover(me, input.scopeType(), input.scopeId());
            }
            List<Item> items = new ArrayList<>();
            for (LocalDate d : new TreeSet<>(input.dates())) {
                if (!d.isAfter(time.today())) {
                    throw new BusinessException(
                            HttpStatus.CONFLICT, "INVALID_SHIFT", "Chỉ xin nghỉ theo ngày cho ngày trong tương lai");
                }
                if (monthPublished(input.scopeType(), input.scopeId(), YearMonth.from(d))) {
                    throw new BusinessException(
                            HttpStatus.CONFLICT,
                            "MONTH_ALREADY_PUBLISHED",
                            "Lịch tháng " + YearMonth.from(d) + " đã công bố; xin nghỉ theo ca");
                }
                if (records.hasLeaveDay(target, d)) {
                    throw new BusinessException(
                            HttpStatus.CONFLICT, "LEAVE_DAY_EXISTS", "Đã có nghỉ theo ngày được duyệt cho ngày " + d);
                }
                items.add(new Item(null, d, Result.PENDING, null, null, null));
            }
            created.add(file(input, target, onBehalf, actorAccount, input.scopeType(), input.scopeId(), items));
        }
        return created;
    }

    private long file(
            LeaveInput input,
            long target,
            boolean onBehalf,
            long actorAccount,
            String scopeType,
            Long scopeId,
            List<Item> items) {
        Long floor = "FLOOR".equals(scopeType) || "AREA".equals(scopeType) ? scopeId : null;
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("kind", input.kind().name());
        content.put("staffId", target);
        content.put("scopeType", scopeType);
        content.put("scopeId", scopeId == null ? json.nullNode() : json.valueToTree(scopeId));
        content.put("floorAreaId", floor == null ? json.nullNode() : json.valueToTree(floor));
        ApprovalRequestView request =
                approvals.create(new CreateRequest(TYPE, json.valueToTree(content), time.today(), input.reason(), true));
        long id = request.id();
        for (Item item : items) {
            if (item.shiftId() != null) {
                records.acquire(item.shiftId(), target, id, actorAccount);
            } else {
                records.acquireDay(target, item.date(), id, actorAccount);
            }
        }
        records.createLeave(
                new LeaveDetail(
                        id,
                        input.kind(),
                        target,
                        scopeType,
                        scopeId,
                        actorAccount,
                        onBehalf,
                        input.reason().value(),
                        items,
                        null,
                        false,
                        null,
                        null),
                SwapStatus.PENDING_APPROVAL,
                actorAccount);
        List<Long> recipients = onBehalf ? List.of(target) : List.of();
        notices.send(
                "M09.LEAVE_REQUEST",
                id,
                "LEAVE_REQUESTED",
                onBehalf ? "Yêu cầu nghỉ đã được lập thay cho bạn" : "Có yêu cầu nghỉ đột xuất cần duyệt",
                recipients,
                floor,
                !onBehalf,
                scopeId == null && !onBehalf);
        return id;
    }

    private List<WorkShift> collectShifts(LeaveInput input, long staffId) {
        Map<Long, WorkShift> byId = new LinkedHashMap<>();
        for (long id : input.shiftIds()) {
            byId.put(id, shifts.shift(id, false));
        }
        if (input.fromDate() != null) {
            LocalDate to = input.toDate() == null ? input.fromDate() : input.toDate();
            Instant from = time.startOfDay(input.fromDate());
            Instant end = time.startOfDay(to.plusDays(1));
            for (WorkShift s : shifts.staffRange(staffId, from, end, true)) {
                byId.putIfAbsent(s.id(), s);
            }
        }
        return List.copyOf(byId.values());
    }

    private void requireLeavable(WorkShift s, long staffId) {
        if (!"PUBLISHED".equals(s.status()) || !s.startsAt().isAfter(time.now())) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_SHIFT", "Ca phải Đã công bố và chưa bắt đầu");
        }
        boolean present = shifts.members(s.id()).stream()
                .anyMatch(m -> m.staffId() == staffId && m.presentAt(time.now()));
        if (!present) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_SHIFT", "Nhân viên không có tên trong ca");
        }
    }

    private boolean monthPublished(String scopeType, Long scopeId, YearMonth month) {
        return shifts.schedules(month, scopeId).stream()
                .anyMatch(s -> Objects.equals(s.scopeType(), scopeType)
                        && Objects.equals(s.scopeId(), scopeId)
                        && !"DRAFT".equals(s.status()));
    }

    // ---- Duyệt, từ chối, hủy ---------------------------------------------------------------------------------------

    @Transactional
    public List<Map<String, Object>> approve(long id, Decision decision) {
        SimpleRecord<LeaveDetail> row = records.lockLeave(id);
        LeaveDetail d = row.content();
        requireStatus(id, SwapStatus.PENDING_APPROVAL);
        CurrentUser me = users.getObject();
        requireApprover(me, d.scopeType(), d.scopeId());
        boolean self = me.staffId().filter(s -> s == d.staffId()).isPresent();
        staff.lock(d.staffId());
        Map<String, ItemDecision> decisions = new LinkedHashMap<>();
        if (decision.items() != null) {
            decision.items().forEach(i -> decisions.put(key(i.shiftId(), i.date()), i));
        }
        boolean anyRejected = false;
        List<Item> out = new ArrayList<>();
        List<Map<String, Object>> effects = new ArrayList<>();
        Basis basis = new Basis("FR-029", id, null, null);
        for (Item item : d.items()) {
            if (item.result() != Result.PENDING) {
                out.add(item);
                continue;
            }
            ItemDecision choice = decisions.get(key(item.shiftId(), item.date()));
            boolean approved = choice == null || choice.approved();
            if (!approved) {
                anyRejected = true;
                out.add(item.with(Result.REJECTED, decision.reason(), null, null));
                continue;
            }
            out.add(item.shiftId() != null
                    ? applyShift(d, item, choice == null ? null : choice.newLeadId(), id, basis, effects)
                    : applyDay(d, item, id, basis, effects));
        }
        if ((anyRejected || self) && (decision.reason() == null || decision.reason().isBlank())) {
            throw new ReasonRequiredException();
        }
        releaseResolved(d, out);
        boolean anyApplied = out.stream().anyMatch(i -> i.result() == Result.APPROVED);
        boolean anyRejectedFinal = out.stream().anyMatch(i -> i.result() == Result.REJECTED);
        String transition = !anyApplied ? LeaveHandler.REJECT : anyRejectedFinal ? LeaveHandler.APPLY_PARTIAL : LeaveHandler.APPLY;
        String status = !anyApplied
                ? SwapStatus.REJECTED
                : anyRejectedFinal ? SwapStatus.PARTIALLY_APPLIED : SwapStatus.APPLIED;
        approvals.applyExtendedTransition(id, transition, basis);
        records.updateLeave(row, status, d.withItems(out, me.accountId(), self), true);
        Reason logged = decision.reason() == null || decision.reason().isBlank() ? null : new Reason(decision.reason());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("LEAVE_DECIDED", self))
                .entity("M09.LEAVE_REQUEST", id)
                .before(Map.of("status", SwapStatus.PENDING_APPROVAL))
                .after(Map.of("status", status))
                .reason(logged)
                .basis(basis)
                .details(Map.of("selfApproved", self))
                .build());
        notices.send(
                "M09.LEAVE_REQUEST",
                id,
                "LEAVE_DECIDED",
                "Yêu cầu nghỉ đã được quyết định",
                d.onBehalf() ? List.of(d.staffId()) : List.of(d.staffId()),
                null,
                false,
                false);
        return effects;
    }

    @Transactional
    public void reject(long id, Reason reason) {
        decide(id, reason);
    }

    private void decide(long id, Reason reason) {
        SimpleRecord<LeaveDetail> row = records.lockLeave(id);
        LeaveDetail d = row.content();
        requireStatus(id, SwapStatus.PENDING_APPROVAL);
        CurrentUser me = users.getObject();
        requireApprover(me, d.scopeType(), d.scopeId());
        List<Item> out = d.items().stream()
                .map(i -> i.result() == Result.PENDING ? i.with(Result.REJECTED, reason.value(), null, null) : i)
                .toList();
        releaseResolved(d, out);
        approvals.applyExtendedTransition(id, LeaveHandler.REJECT, new Basis("FR-028", id, null, null));
        records.updateLeave(
                row,
                SwapStatus.REJECTED,
                d.withItems(out, me.accountId(), me.staffId().filter(s -> s == d.staffId()).isPresent()),
                true);
        notices.send(
                "M09.LEAVE_REQUEST",
                id,
                "LEAVE_DECIDED",
                "Yêu cầu nghỉ bị từ chối",
                List.of(d.staffId()),
                null,
                false,
                false);
    }

    /** FR-031: người nghỉ hoặc người lập thay hủy khi còn Chờ duyệt. */
    @Transactional
    public void cancel(long id, Reason reason) {
        SimpleRecord<LeaveDetail> row = records.lockLeave(id);
        LeaveDetail d = row.content();
        requireStatus(id, SwapStatus.PENDING_APPROVAL);
        CurrentUser me = users.getObject();
        boolean owner = me.staffId().filter(s -> s == d.staffId()).isPresent();
        if (!owner && me.accountId() != d.filedByAccountId()) {
            throw new PermissionDeniedException("Chỉ người nghỉ hoặc người lập thay được hủy");
        }
        releaseResolved(d, d.items().stream().map(i -> i.with(Result.LEFT, null, null, null)).toList());
        approvals.applyExtendedTransition(id, LeaveHandler.CANCEL, new Basis("FR-031", id, null, null));
        records.updateLeave(row, SwapStatus.CANCELLED, d, true);
    }

    @Transactional
    public void syncClosedByGenericCommand(long requestId, String status) {
        records.findLeave(requestId).ifPresent(row -> {
            if (SwapStatus.PENDING_APPROVAL.equals(row.status())) {
                releaseResolved(
                        row.content(),
                        row.content().items().stream()
                                .map(i -> i.with(Result.LEFT, null, null, null))
                                .toList());
                records.updateLeave(row, status, row.content(), true);
            }
        });
    }

    // ---- Tác động -------------------------------------------------------------------------------------------------

    /** FR-029: một ca được duyệt nghỉ. */
    private Item applyShift(
            LeaveDetail d, Item item, Long chosenLead, long requestId, Basis basis, List<Map<String, Object>> effects) {
        WorkShift s = shifts.shift(item.shiftId(), true);
        boolean present = shifts.members(s.id()).stream()
                .anyMatch(m -> m.staffId() == d.staffId() && m.presentAt(time.now()));
        if (!present || !"PUBLISHED".equals(s.status()) || !s.startsAt().isAfter(time.now())) {
            return item.with(Result.LEFT, null, null, "Ca không còn thỏa lúc duyệt");
        }
        Long newLead = null;
        boolean wasLead = Objects.equals(s.leadStaffId(), d.staffId());
        if (wasLead) {
            List<Long> candidates = eligibleOthers(s, d.staffId());
            if (chosenLead != null) {
                if (!candidates.contains(chosenLead)) {
                    throw new BusinessException(
                            HttpStatus.CONFLICT, "LEAD_REQUIRED", "Người phụ trách được chọn không đủ điều kiện");
                }
                newLead = chosenLead;
            } else if (!candidates.isEmpty()) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "LEAD_REQUIRED",
                        "Cần chọn người phụ trách mới cho ca #" + s.id(),
                        Map.of("shiftId", s.id()));
            }
        }
        shiftPort.leaveShift(s.id(), d.staffId(), new ShiftDepartureBasis(ShiftDepartureBasis.Kind.APPROVED_LEAVE, requestId));
        if (wasLead) {
            if (newLead != null) {
                shiftPort.changeLead(s.id(), newLead, basis);
            } else {
                shiftPort.markMissingLead(s.id(), basis);
            }
        }
        tasks.ifAvailable(api -> api.onApprovedLeave(d.staffId(), Set.of(s.id())));
        tripCommands.ifAvailable(api -> api.removeDuty(d.staffId(), s.id(), basis));
        effects.add(Map.of("shiftId", s.id(), "leadMissing", wasLead && newLead == null));
        return item.with(Result.APPROVED, null, newLead, null);
    }

    /** FR-026a: gỡ khỏi ca Nháp bắt đầu trong ngày, người phụ trách để trống; ghi nghỉ theo ngày. */
    private Item applyDay(LeaveDetail d, Item item, long requestId, Basis basis, List<Map<String, Object>> effects) {
        LocalDate date = item.date();
        Instant from = time.startOfDay(date);
        Instant to = time.startOfDay(date.plusDays(1));
        for (WorkShift s : shifts.forStaff(d.staffId(), null, true)) {
            if (!"DRAFT".equals(s.status()) || s.startsAt().isBefore(from) || !s.startsAt().isBefore(to)) {
                continue;
            }
            WorkShift draft = shifts.shift(s.id(), true);
            List<ShiftMember> members = shifts.members(draft.id(), true);
            List<ShiftInput.Member> remaining = members.stream()
                    .filter(m -> m.staffId() != d.staffId())
                    .map(m -> new ShiftInput.Member(m.staffId(), m.onDutyDoctor()))
                    .toList();
            boolean leadCleared = Objects.equals(draft.leadStaffId(), d.staffId());
            assignmentCommands.removeDraftMembers(
                    draft.id(), remaining.stream().map(ShiftInput.Member::staffId).toList());
            shifts.updateShift(
                    draft,
                    shifts.template(draft.shiftTemplateId(), false),
                    new ShiftInput(
                            draft.shiftTemplateId(),
                            date,
                            draft.startsAt(),
                            draft.endsAt(),
                            remaining,
                            leadCleared ? null : draft.leadStaffId(),
                            true,
                            null),
                    draft.startsAt(),
                    draft.endsAt());
            effects.add(Map.of("shiftId", draft.id(), "draft", true, "leadCleared", leadCleared));
        }
        records.recordLeaveDay(new LeaveDay(d.staffId(), date, requestId), users.getObject().accountId());
        return item.with(Result.APPROVED, null, null, null);
    }

    private List<Long> eligibleOthers(WorkShift s, long leaving) {
        return shifts.members(s.id()).stream()
                .filter(m -> m.staffId() != leaving && m.presentAt(time.now()))
                .map(ShiftMember::staffId)
                .filter(this::eligibleLead)
                .toList();
    }

    private boolean eligibleLead(long staffId) {
        return staff.find(staffId).filter(p -> "WORKING".equals(p.status())).isPresent()
                && accounts.byStaff(staffId)
                        .filter(a -> a.status() != AccountStatus.LOCKED
                                && a.roles().stream().anyMatch(r -> r == Role.FLOOR_MANAGER || r == Role.NURSING_STAFF))
                        .isPresent();
    }

    /** Nhả khóa của các mục không còn chờ (đã quyết định, rời yêu cầu, tách). */
    private void releaseResolved(LeaveDetail d, List<Item> items) {
        for (Item i : items) {
            if (i.result() == Result.PENDING) {
                continue;
            }
            if (i.shiftId() != null) {
                records.release(i.shiftId(), d.staffId());
            } else {
                records.releaseDay(d.staffId(), i.date());
            }
        }
    }

    // ---- Job ---------------------------------------------------------------------------------------------------

    /** FR-030: ca tới giờ bắt đầu mà chưa quyết định thì tách khỏi yêu cầu; hết mục thì Hết hiệu lực. */
    @Transactional
    public int expireDue() {
        int count = 0;
        for (SimpleRecord<LeaveDetail> row : records.leaves(Set.of(SwapStatus.PENDING_APPROVAL))) {
            SimpleRecord<LeaveDetail> locked = records.lockLeave(row.content().requestId());
            LeaveDetail d = locked.content();
            if (!SwapStatus.PENDING_APPROVAL.equals(locked.status())) {
                continue;
            }
            boolean changed = false;
            List<Item> out = new ArrayList<>();
            for (Item i : d.items()) {
                if (i.result() == Result.PENDING && started(i)) {
                    out.add(i.with(Result.SPLIT_OFF, null, null, "Ca bắt đầu khi chưa quyết định"));
                    changed = true;
                } else if (i.result() == Result.PENDING && i.shiftId() == null && !i.date().isAfter(time.today())) {
                    out.add(i.with(Result.SPLIT_OFF, null, null, "Ngày nghỉ đã tới khi chưa quyết định"));
                    changed = true;
                } else {
                    out.add(i);
                }
            }
            if (!changed) {
                continue;
            }
            releaseResolved(d, out);
            boolean none = out.stream().noneMatch(i -> i.result() == Result.PENDING);
            if (none) {
                approvals.applyExtendedTransition(
                        d.requestId(), LeaveHandler.EXPIRE, new Basis("FR-030", d.requestId(), null, null));
                records.updateLeave(locked, SwapStatus.EXPIRED, d.withItems(out, null, false), true);
                notices.send(
                        "M09.LEAVE_REQUEST",
                        d.requestId(),
                        "LEAVE_EXPIRED",
                        "Yêu cầu nghỉ đã hết hiệu lực",
                        List.of(d.staffId()),
                        null,
                        false,
                        false);
            } else {
                records.updateLeave(locked, locked.status(), d.withItems(out, null, false), false);
            }
            count++;
        }
        return count;
    }

    private boolean started(Item i) {
        return i.shiftId() != null
                && shifts.find(i.shiftId())
                        .map(s -> !s.startsAt().isAfter(time.now()) || "CANCELLED".equals(s.status()))
                        .orElse(true);
    }

    /** FR-030 (nhắc theo CFG-M09-11) tính theo mục chờ sớm nhất; mỗi yêu cầu một lần. */
    @Transactional
    public int remindDue() {
        Duration window = parameters.read(ParameterCode.CFG_M09_11).asDuration();
        int count = 0;
        for (SimpleRecord<LeaveDetail> row : records.leaves(Set.of(SwapStatus.PENDING_APPROVAL))) {
            LeaveDetail d = row.content();
            Instant earliest = d.items().stream()
                    .filter(i -> i.result() == Result.PENDING)
                    .map(this::startOf)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            if (earliest == null || earliest.minus(window).isAfter(time.now())) {
                continue;
            }
            if (records.markLeaveReminded(d.requestId(), time.now())) {
                notices.send(
                        "M09.LEAVE_REQUEST",
                        d.requestId(),
                        "REQUEST_DUE_SOON",
                        "Yêu cầu nghỉ sắp tới giờ ca mà chưa được quyết định",
                        List.of(d.staffId()),
                        "FLOOR".equals(d.scopeType()) || "AREA".equals(d.scopeType()) ? d.scopeId() : null,
                        true,
                        d.scopeId() == null);
                count++;
            }
        }
        return count;
    }

    private Instant startOf(Item i) {
        return i.shiftId() != null
                ? shifts.find(i.shiftId()).map(WorkShift::startsAt).orElse(time.now())
                : time.startOfDay(i.date());
    }

    // ---- Lịch tháng công bố: nghỉ theo ngày chuyển theo ca (FR-026a, Q-184) --------------------------------------

    @EventListener
    @Transactional
    public void onSchedulePublished(SchedulePublished event) {
        ShiftSchedule schedule = shifts.schedule(event.scheduleId(), false);
        List<WorkShift> published = shifts.scheduleShifts(schedule.id());
        for (SimpleRecord<LeaveDetail> row : records.leaves(Set.of(SwapStatus.PENDING_APPROVAL))) {
            LeaveDetail d = row.content();
            if (d.kind() != LeaveKind.BY_DAY
                    || !Objects.equals(d.scopeType(), schedule.scopeType())
                    || !Objects.equals(d.scopeId(), schedule.scopeId())) {
                continue;
            }
            List<Item> converted = new ArrayList<>();
            boolean touched = false;
            for (Item i : d.items()) {
                if (i.result() != Result.PENDING || !YearMonth.from(i.date()).equals(schedule.month())) {
                    converted.add(i);
                    continue;
                }
                touched = true;
                records.releaseDay(d.staffId(), i.date());
                Instant from = time.startOfDay(i.date());
                Instant to = time.startOfDay(i.date().plusDays(1));
                for (WorkShift s : published) {
                    boolean mine = !s.startsAt().isBefore(from)
                            && s.startsAt().isBefore(to)
                            && shifts.members(s.id()).stream().anyMatch(m -> m.staffId() == d.staffId());
                    if (mine) {
                        records.acquire(s.id(), d.staffId(), d.requestId(), d.filedByAccountId());
                        converted.add(new Item(s.id(), null, Result.PENDING, null, null, null));
                    }
                }
            }
            if (touched) {
                records.updateLeave(
                        row,
                        row.status(),
                        d.withKind(
                                converted.stream().anyMatch(i -> i.shiftId() != null) ? LeaveKind.BY_SHIFT : d.kind(),
                                converted),
                        false);
            }
        }
    }

    // ---- ApprovedLeaveDayPort (008 FR-012, FR-016) -----------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean approvedLeaveDay(long staffId, LocalDate date) {
        return records.hasLeaveDay(staffId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> pendingLeaveDayStaff(String scopeType, Long scopeId, YearMonth month) {
        return records.leaves(Set.of(SwapStatus.PENDING_APPROVAL)).stream()
                .map(SimpleRecord::content)
                .filter(d -> d.kind() == LeaveKind.BY_DAY
                        && Objects.equals(d.scopeType(), scopeType)
                        && Objects.equals(d.scopeId(), scopeId)
                        && d.items().stream()
                                .anyMatch(i -> i.result() == Result.PENDING
                                        && YearMonth.from(i.date()).equals(month)))
                .map(LeaveDetail::staffId)
                .distinct()
                .toList();
    }

    // ---- Đọc -------------------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LeaveView> list() {
        CurrentUser me = users.getObject();
        List<LeaveView> out = new ArrayList<>();
        for (SimpleRecord<LeaveDetail> row : records.leaves(allStatuses())) {
            LeaveDetail d = row.content();
            if (involved(me, d) || mayApprove(me, d.scopeType(), d.scopeId())) {
                out.add(view(row, me));
            }
        }
        out.sort(Comparator.comparingLong(LeaveView::id).reversed());
        return out;
    }

    @Transactional(readOnly = true)
    public LeaveView get(long id) {
        CurrentUser me = users.getObject();
        SimpleRecord<LeaveDetail> row = records.leave(id);
        LeaveDetail d = row.content();
        if (!involved(me, d) && !mayApprove(me, d.scopeType(), d.scopeId())) {
            throw new NotFoundException();
        }
        return view(row, me);
    }

    /** FR-027: tác động của từng ca trước khi duyệt; chỉ người duyệt. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> preApproval(long id) {
        CurrentUser me = users.getObject();
        LeaveDetail d = records.leave(id).content();
        if (!mayApprove(me, d.scopeType(), d.scopeId())) {
            throw new NotFoundException();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Item i : d.items()) {
            if (i.shiftId() == null) {
                continue;
            }
            WorkShift s = shifts.shift(i.shiftId(), false);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("shiftId", s.id());
            m.put("startsAt", s.startsAt());
            m.put("endsAt", s.endsAt());
            m.put("isLead", Objects.equals(s.leadStaffId(), d.staffId()));
            m.put("eligibleLeadCandidates", Objects.equals(s.leadStaffId(), d.staffId()) ? eligibleOthers(s, d.staffId()) : List.of());
            m.put(
                    "assignments",
                    assignments.inShift(s.id(), false).stream()
                            .filter(a -> a.staffId() == d.staffId() && !Set.of("CANCELLED", "ENDED").contains(a.status()))
                            .map(StaffAssignment::id)
                            .toList());
            List<Map<String, Object>> overlapping = new ArrayList<>();
            trips.ifAvailable(port -> {
                if (s.scopeId() != null) {
                    port.inShift(s.scopeId(), s.startsAt(), s.endsAt()).stream()
                            .filter(t -> t.staffIds().contains(d.staffId()))
                            .forEach(t -> overlapping.add(Map.of("tripId", t.tripId())));
                }
            });
            m.put("overlappingTrips", overlapping);
            out.add(m);
        }
        return out;
    }

    private LeaveView view(SimpleRecord<LeaveDetail> row, CurrentUser me) {
        LeaveDetail d = row.content();
        boolean owner = me.staffId().filter(s -> s == d.staffId()).isPresent();
        boolean filer = me.accountId() == d.filedByAccountId();
        boolean approver = mayApprove(me, d.scopeType(), d.scopeId());
        List<String> commands = new ArrayList<>();
        if (SwapStatus.PENDING_APPROVAL.equals(row.status())) {
            if (owner || filer) {
                commands.add("cancel");
            }
            if (approver) {
                commands.add("approve");
                commands.add("reject");
            }
        }
        return new LeaveView(
                d.requestId(),
                row.status(),
                d.kind(),
                d.staffId(),
                d.scopeType(),
                d.scopeId(),
                d.onBehalf(),
                owner || filer || approver ? d.reason() : null,
                d.items(),
                d.selfApproved(),
                List.copyOf(commands));
    }

    private boolean involved(CurrentUser me, LeaveDetail d) {
        return me.staffId().filter(s -> s == d.staffId()).isPresent() || me.accountId() == d.filedByAccountId();
    }

    // ---- Quyền và tiện ích -------------------------------------------------------------------------------------------

    /** FR-028: Trưởng tầng được giao của phạm vi; phạm vi toàn viện: Quản lý viện (Q-177). */
    private boolean mayApprove(CurrentUser me, String scopeType, Long scopeId) {
        if (me.roles().contains(Role.NURSING_HOME_MANAGER)) {
            return true;
        }
        return scopeId != null
                && me.roles().contains(Role.FLOOR_MANAGER)
                && permissions
                        .hasPermission(
                                me,
                                new PermissionKey(Function.SHIFT_SWAP_REQUEST, Operation.APPROVE),
                                new Target("M09.LEAVE_REQUEST", null, null, scopeId))
                        .allowed();
    }

    private void requireApprover(CurrentUser me, String scopeType, Long scopeId) {
        if (!mayApprove(me, scopeType, scopeId)) {
            throw new PermissionDeniedException("Bạn không có quyền duyệt hoặc lập thay yêu cầu nghỉ này");
        }
    }

    private void requireStatus(long id, String expected) {
        String status = approvals.find(id).orElseThrow(NotFoundException::new).status();
        if (!expected.equals(status)) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Yêu cầu đã đổi trạng thái; tải lại");
        }
    }

    private static String key(Long shiftId, LocalDate date) {
        return shiftId != null ? "S" + shiftId : "D" + date;
    }

    private static Set<String> allStatuses() {
        return Set.of(
                SwapStatus.PENDING_APPROVAL,
                SwapStatus.APPLIED,
                SwapStatus.PARTIALLY_APPLIED,
                SwapStatus.REJECTED,
                SwapStatus.CANCELLED,
                SwapStatus.EXPIRED);
    }
}
