package com.eldercare.backend.staffing.internal.swap.service;

import static com.eldercare.backend.staffing.internal.swap.service.SwapHandler.TYPE;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountView;
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
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.spi.TripDutyPort;
import com.eldercare.backend.staffing.internal.assignment.domain.AssignmentInput;
import com.eldercare.backend.staffing.internal.assignment.domain.StaffAssignment;
import com.eldercare.backend.staffing.internal.assignment.persistence.AssignmentStore;
import com.eldercare.backend.staffing.internal.assignment.service.AssignmentCommandPort;
import com.eldercare.backend.staffing.internal.shift.domain.ContinuousShiftChain;
import com.eldercare.backend.staffing.internal.shift.domain.ContinuousShiftChain.Interval;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.shift.service.ShiftCommandPort;
import com.eldercare.backend.staffing.internal.shift.service.ShiftDepartureBasis;
import com.eldercare.backend.staffing.internal.shift.service.ShiftEntryBasis;
import com.eldercare.backend.staffing.internal.staff.persistence.StaffStore;
import com.eldercare.backend.staffing.internal.swap.domain.RecipientShift;
import com.eldercare.backend.staffing.internal.swap.domain.SwapChecks;
import com.eldercare.backend.staffing.internal.swap.domain.SwapDetail;
import com.eldercare.backend.staffing.internal.swap.domain.SwapKind;
import com.eldercare.backend.staffing.internal.swap.domain.SwapRules;
import com.eldercare.backend.staffing.internal.swap.domain.SwapStatus;
import com.eldercare.backend.staffing.internal.swap.domain.SwapView;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đổi ca (015 FR-019 → FR-025; BR-M09-10): lập, đồng ý, từ chối, rút đồng ý, hủy, duyệt, từ chối duyệt; mọi tác động
 * khi duyệt trong một transaction qua port lệnh nội bộ của 008 (N-07). Vòng đời là trạng thái mở rộng của yêu cầu phê
 * duyệt (research X-08); chi tiết đổi được ở {@code simple_record}.
 */
@Service
public class SwapService {

    private static final Set<String> BLOCKING_SHIFT = Set.of("PUBLISHED");

    private final ShiftStore shifts;
    private final StaffStore staff;
    private final AssignmentStore assignments;
    private final AccountApi accounts;
    private final ParameterApi parameters;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> users;
    private final BusinessTime time;
    private final ApprovalRequestApi approvals;
    private final SwapRecords records;
    private final ShiftCommandPort shiftPort;
    private final AssignmentCommandPort assignmentPort;
    private final ObjectProvider<ShiftTaskApi> tasks;
    private final ObjectProvider<TripDutyCommandApi> tripCommands;
    private final ObjectProvider<TripDutyPort> trips;
    private final AuditLogApi audit;
    private final SwapNotices notices;
    private final JsonMapper json;

    public SwapService(
            ShiftStore shifts,
            StaffStore staff,
            AssignmentStore assignments,
            AccountApi accounts,
            ParameterApi parameters,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> users,
            BusinessTime time,
            ApprovalRequestApi approvals,
            SwapRecords records,
            ShiftCommandPort shiftPort,
            AssignmentCommandPort assignmentPort,
            ObjectProvider<ShiftTaskApi> tasks,
            ObjectProvider<TripDutyCommandApi> tripCommands,
            ObjectProvider<TripDutyPort> trips,
            AuditLogApi audit,
            SwapNotices notices,
            JsonMapper json) {
        this.shifts = shifts;
        this.staff = staff;
        this.assignments = assignments;
        this.accounts = accounts;
        this.parameters = parameters;
        this.permissions = permissions;
        this.users = users;
        this.time = time;
        this.approvals = approvals;
        this.records = records;
        this.shiftPort = shiftPort;
        this.assignmentPort = assignmentPort;
        this.tasks = tasks;
        this.tripCommands = tripCommands;
        this.trips = trips;
        this.audit = audit;
        this.notices = notices;
        this.json = json;
    }

    /** Lệnh duyệt gửi kèm: lý do, người phụ trách chọn, xác nhận cảnh báo. */
    public record Decision(String reason, Long newLeadXId, Long newLeadYId, boolean confirmWarning) {}

    public record Created(long id, SwapChecks checks) {}

    // ---- Lập ---------------------------------------------------------------------------------------------------

    @Transactional
    public Created create(SwapKind kind, long shiftXId, long recipientId, Long shiftYId, Reason reason) {
        long giver = currentStaff();
        if (kind == SwapKind.MUTUAL && shiftYId == null) {
            throw invalidShift("Đổi hai chiều cần chọn ca của người nhận");
        }
        if (kind == SwapKind.TAKE_OVER && shiftYId != null) {
            throw invalidShift("Nhận thay không có ca của người nhận");
        }
        lockStaff(giver, recipientId);
        WorkShift x = lockShifts(shiftXId, shiftYId).get(shiftXId);
        WorkShift y = shiftYId == null ? null : shifts.shift(shiftYId, false);
        requireSwappable(x);
        requirePresent(x, giver, true);
        if (y != null) {
            requireSwappable(y);
            if (!Objects.equals(x.scopeType(), y.scopeType()) || !Objects.equals(x.scopeId(), y.scopeId())) {
                throw new BusinessException(
                        HttpStatus.CONFLICT, "DIFFERENT_SCOPE", "Hai ca khác phạm vi; dùng Bổ sung nhân viên vào ca");
            }
            requirePresent(y, recipientId, false);
            if (inShift(y, giver)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT, "INVALID_RECIPIENT", "Người nhường đã có tên trong ca của người nhận");
            }
        }
        requireRecipient(giver, recipientId, x);
        long actor = users.getObject().accountId();
        SwapDetail detail = new SwapDetail(
                0, kind, giver, x.id(), recipientId, y == null ? null : y.id(), reason.value(), null, null, null, null,
                false, earliest(x, y), null, null);
        SwapChecks checks = evaluate(detail, false);
        if (checks.blocked()) {
            throw new ConditionNotMetException(overlapConditions(checks));
        }
        ApprovalRequestView request = approvals.create(new CreateRequest(
                TYPE,
                json.valueToTree(Map.of(
                        "kind", kind.name(),
                        "shiftXId", x.id(),
                        "recipientId", recipientId,
                        "shiftYId", y == null ? json.nullNode() : json.valueToTree(y.id()),
                        "giverStaffId", giver,
                        "floorAreaId", x.scopeId() == null ? json.nullNode() : json.valueToTree(x.scopeId()))),
                time.today(),
                reason,
                true));
        long id = request.id();
        detail = new SwapDetail(
                id, kind, giver, x.id(), recipientId, detail.shiftYId(), reason.value(), null, null, null, null, false,
                detail.earliestStart(), null, null);
        records.acquire(x.id(), giver, id, actor);
        if (y != null) {
            records.acquire(y.id(), recipientId, id, actor);
        }
        records.createSwap(detail, SwapStatus.AWAITING_RECIPIENT, actor);
        approvals.applyExtendedTransition(id, SwapHandler.WAIT_RECIPIENT, new Basis("FR-022", id, null, null));
        notices.send(
                "M09.SHIFT_SWAP",
                id,
                "SWAP_REQUESTED",
                "Bạn có yêu cầu đổi ca cần phản hồi",
                List.of(recipientId),
                x.scopeId(),
                false,
                false);
        return new Created(id, checks);
    }

    // ---- Người nhận --------------------------------------------------------------------------------------------

    @Transactional
    public SwapChecks accept(long id) {
        long me = currentStaff();
        SimpleRecord<SwapDetail> row = records.lockSwap(id);
        SwapDetail d = row.content();
        requireStatus(id, SwapStatus.AWAITING_RECIPIENT);
        requireIs(me, d.recipientStaffId());
        lockStaff(d.giverStaffId(), d.recipientStaffId());
        ensureStillValid(d);
        SwapChecks checks = evaluate(d, false);
        if (checks.blocked()) {
            throw new ConditionNotMetException(overlapConditions(checks));
        }
        approvals.applyExtendedTransition(id, SwapHandler.RECIPIENT_AGREED, new Basis("FR-022", id, null, null));
        records.updateSwap(row, SwapStatus.PENDING_APPROVAL, d.reminded(null), false);
        WorkShift x = shifts.shift(d.shiftXId(), false);
        notices.send(
                "M09.SHIFT_SWAP",
                id,
                "SWAP_ACCEPTED",
                "Người nhận đã đồng ý yêu cầu đổi ca, chờ duyệt",
                List.of(d.giverStaffId()),
                x.scopeId(),
                true,
                false);
        return checks.withoutApproverOnly();
    }

    @Transactional
    public void decline(long id, Reason reason) {
        long me = currentStaff();
        SimpleRecord<SwapDetail> row = records.lockSwap(id);
        SwapDetail d = row.content();
        requireStatus(id, SwapStatus.AWAITING_RECIPIENT);
        requireIs(me, d.recipientStaffId());
        closeAsDeclined(id, row, d, reason.value());
    }

    @Transactional
    public void withdraw(long id, Reason reason) {
        long me = currentStaff();
        SimpleRecord<SwapDetail> row = records.lockSwap(id);
        SwapDetail d = row.content();
        requireStatus(id, SwapStatus.PENDING_APPROVAL);
        requireIs(me, d.recipientStaffId());
        closeAsDeclined(id, row, d, reason == null ? null : reason.value());
    }

    private void closeAsDeclined(long id, SimpleRecord<SwapDetail> row, SwapDetail d, String declineReason) {
        approvals.applyExtendedTransition(id, SwapHandler.RECIPIENT_DECLINED, new Basis("FR-022", id, null, null));
        records.updateSwap(row, SwapStatus.RECIPIENT_DECLINED, d.declined(declineReason), true);
        releaseLocks(d);
        notices.send(
                "M09.SHIFT_SWAP",
                id,
                "SWAP_DECLINED",
                "Người nhận không đồng ý yêu cầu đổi ca",
                List.of(d.giverStaffId()),
                null,
                false,
                false);
    }

    // ---- Người nhường ------------------------------------------------------------------------------------------

    @Transactional
    public void cancel(long id, Reason reason) {
        long me = currentStaff();
        SimpleRecord<SwapDetail> row = records.lockSwap(id);
        SwapDetail d = row.content();
        if (!SwapStatus.isOpen(statusOf(id))) {
            throw stateChanged();
        }
        requireIs(me, d.giverStaffId());
        approvals.applyExtendedTransition(id, SwapHandler.CANCEL, new Basis("FR-022", id, null, null));
        records.updateSwap(row, SwapStatus.CANCELLED, d, true);
        releaseLocks(d);
        notices.send(
                "M09.SHIFT_SWAP",
                id,
                "SWAP_CANCELLED",
                "Yêu cầu đổi ca đã được rút lại",
                List.of(d.recipientStaffId()),
                null,
                false,
                false);
    }

    // ---- Người duyệt -------------------------------------------------------------------------------------------

    @Transactional
    public void reject(long id, Reason reason) {
        SimpleRecord<SwapDetail> row = records.lockSwap(id);
        SwapDetail d = row.content();
        requireStatus(id, SwapStatus.PENDING_APPROVAL);
        WorkShift x = shifts.shift(d.shiftXId(), false);
        requireApprover(x, d);
        approvals.applyExtendedTransition(id, SwapHandler.REJECT, new Basis("FR-023", id, null, null));
        records.updateSwap(
                row,
                SwapStatus.REJECTED,
                d.decided(users.getObject().accountId(), null, null, isParty(d, users.getObject())),
                true);
        releaseLocks(d);
        notices.send(
                "M09.SHIFT_SWAP",
                id,
                "SWAP_REJECTED",
                "Yêu cầu đổi ca bị từ chối",
                List.of(d.giverStaffId(), d.recipientStaffId()),
                null,
                false,
                false);
    }

    @Transactional
    public List<Map<String, Object>> approve(long id, Decision decision) {
        SimpleRecord<SwapDetail> row = records.lockSwap(id);
        SwapDetail d = row.content();
        requireStatus(id, SwapStatus.PENDING_APPROVAL);
        lockStaff(d.giverStaffId(), d.recipientStaffId());
        Map<Long, WorkShift> locked = lockShifts(d.shiftXId(), d.shiftYId());
        WorkShift x = locked.get(d.shiftXId());
        WorkShift y = d.shiftYId() == null ? null : locked.get(d.shiftYId());
        requireApprover(x, d);
        CurrentUser me = users.getObject();
        boolean self = isParty(d, me);
        ensureStillValid(d);
        SwapChecks checks = evaluate(d, true);
        if (checks.blocked()) {
            throw new ConditionNotMetException(overlapConditions(checks));
        }
        boolean needReason = self || !checks.continuousHours().isEmpty();
        if (needReason && (decision.reason() == null || decision.reason().isBlank())) {
            throw new ReasonRequiredException();
        }
        if (!checks.continuousHours().isEmpty() && !decision.confirmWarning()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "WARNING_CONFIRMATION_REQUIRED",
                    "Chuỗi giờ làm liên tục vượt ngưỡng; cần xác nhận cảnh báo",
                    Map.of("warnings", checks.continuousHours()));
        }
        Long leadX = resolveLead(x, d.giverStaffId(), d.recipientStaffId(), decision.newLeadXId(), true);
        Long leadY = y == null ? null : resolveLead(y, d.recipientStaffId(), d.giverStaffId(), decision.newLeadYId(), false);
        Basis basis = new Basis("BR-M09-10", id, null, null);
        Reason entryReason = decision.reason() == null || decision.reason().isBlank() ? null : new Reason(decision.reason());
        List<Map<String, Object>> notTransferred = new ArrayList<>();
        move(x, d.giverStaffId(), d.recipientStaffId(), leadX, id, d.kind(), entryReason, notTransferred, basis);
        if (y != null) {
            move(y, d.recipientStaffId(), d.giverStaffId(), leadY, id, d.kind(), entryReason, notTransferred, basis);
        }
        approvals.applyExtendedTransition(id, SwapHandler.APPLY, basis);
        records.updateSwap(row, SwapStatus.APPLIED, d.decided(me.accountId(), leadX, leadY, self), true);
        releaseLocks(d);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("SHIFT_SWAP_APPLIED", self))
                .entity("M09.SHIFT_SWAP", id)
                .before(Map.of("status", SwapStatus.PENDING_APPROVAL))
                .after(Map.of("status", SwapStatus.APPLIED, "notTransferred", notTransferred.size()))
                .reason(entryReason)
                .basis(basis)
                .details(Map.of("selfApproved", self))
                .build());
        notices.send(
                "M09.SHIFT_SWAP",
                id,
                "SWAP_APPLIED",
                "Yêu cầu đổi ca đã được duyệt và áp dụng",
                List.of(d.giverStaffId(), d.recipientStaffId()),
                x.scopeId(),
                false,
                false);
        return notTransferred;
    }

    /** FR-024 (a) → (c), (g): người nhường rời, người nhận vào, phân công, người phụ trách, nhiệm vụ chuyến đi. */
    private void move(
            WorkShift shift,
            long from,
            long to,
            Long newLead,
            long requestId,
            SwapKind kind,
            Reason reason,
            List<Map<String, Object>> notTransferred,
            Basis basis) {
        ShiftEntryBasis entry = new ShiftEntryBasis(
                kind == SwapKind.MUTUAL ? ShiftEntryBasis.Kind.SWAPPED : ShiftEntryBasis.Kind.COVERED,
                requestId,
                reason);
        shiftPort.addToShift(shift.id(), to, entry, true);
        if (newLead != null) {
            shiftPort.changeLead(shift.id(), newLead, basis);
        }
        List<StaffAssignment> future = assignments.inShift(shift.id(), true).stream()
                .filter(a -> a.staffId() == from && "SCHEDULED".equals(a.status()))
                .toList();
        shiftPort.leaveShift(shift.id(), from, new ShiftDepartureBasis(ShiftDepartureBasis.Kind.SWAPPED, requestId));
        for (StaffAssignment a : future) {
            AssignmentInput input = new AssignmentInput(
                    to,
                    a.targetType(),
                    a.targetId(),
                    a.taskTypeId(),
                    a.role(),
                    a.assignmentKind(),
                    a.assignedNurse(),
                    a.startsAt(),
                    a.endsAt());
            List<UnmetCondition> unmet = assignmentPort.validate(shift.id(), input);
            if (unmet.isEmpty()) {
                assignmentPort.create(shift.id(), input, basis);
            } else {
                notTransferred.add(Map.of(
                        "assignmentId", a.id(), "shiftId", shift.id(), "reason", unmet.get(0).description()));
                tasks.ifAvailable(api -> api.onAssignmentEnded(a.id(), shift.id()));
            }
        }
        tripCommands.ifAvailable(api -> api.removeDuty(from, shift.id(), basis));
    }

    /** Yêu cầu bị hủy hoặc từ chối qua lệnh chung của 000: chép trạng thái sang chi tiết và nhả khóa ca. */
    @Transactional
    public void syncClosedByGenericCommand(long requestId, String status) {
        records.findSwap(requestId).ifPresent(row -> {
            if (SwapStatus.isOpen(row.status())) {
                records.updateSwap(row, status, row.content(), true);
                releaseLocks(row.content());
            }
        });
    }

    // ---- Hết hiệu lực (job) ------------------------------------------------------------------------------------

    /** FR-025, data-model mục 4: tới giờ ca sớm hơn thì hết hiệu lực; ca bị hủy hoặc không còn thỏa cũng vậy. */
    @Transactional
    public int expireDue() {
        int count = 0;
        for (SimpleRecord<SwapDetail> row : records.swaps(Set.of(SwapStatus.AWAITING_RECIPIENT, SwapStatus.PENDING_APPROVAL))) {
            SimpleRecord<SwapDetail> locked = records.lockSwap(row.content().requestId());
            SwapDetail d = locked.content();
            if (!SwapStatus.isOpen(locked.status()) || stillValid(d) && time.now().isBefore(d.earliestStart())) {
                continue;
            }
            approvals.applyExtendedTransition(
                    d.requestId(), SwapHandler.EXPIRE, new Basis("BR-M09-10", d.requestId(), null, null));
            records.updateSwap(locked, SwapStatus.EXPIRED, d, true);
            releaseLocks(d);
            notices.send(
                    "M09.SHIFT_SWAP",
                    d.requestId(),
                    "SWAP_EXPIRED",
                    "Yêu cầu đổi ca đã hết hiệu lực",
                    List.of(d.giverStaffId(), d.recipientStaffId()),
                    null,
                    false,
                    false);
            count++;
        }
        return count;
    }

    /** FR-025: nhắc người đang phải hành động khi ca sớm nhất còn CFG-M09-11 tới giờ bắt đầu; mỗi bước nhắc một lần. */
    @Transactional
    public int remindDue() {
        Duration window = parameters.read(ParameterCode.CFG_M09_11).asDuration();
        int count = 0;
        for (SimpleRecord<SwapDetail> row : records.swaps(Set.of(SwapStatus.AWAITING_RECIPIENT, SwapStatus.PENDING_APPROVAL))) {
            SwapDetail d = row.content();
            if (d.remindedAt() != null || d.earliestStart().minus(window).isAfter(time.now())) {
                continue;
            }
            SimpleRecord<SwapDetail> locked = records.lockSwap(d.requestId());
            if (!SwapStatus.isOpen(locked.status()) || locked.content().remindedAt() != null) {
                continue;
            }
            boolean awaitingRecipient = SwapStatus.AWAITING_RECIPIENT.equals(locked.status());
            WorkShift x = shifts.shift(d.shiftXId(), false);
            notices.send(
                    "M09.SHIFT_SWAP",
                    d.requestId(),
                    "REQUEST_DUE_SOON",
                    "Yêu cầu đổi ca sắp tới giờ ca mà chưa được quyết định",
                    awaitingRecipient ? List.of(d.recipientStaffId(), d.giverStaffId()) : List.of(d.giverStaffId()),
                    x.scopeId(),
                    !awaitingRecipient,
                    false);
            records.updateSwap(locked, locked.status(), locked.content().reminded(time.now()), false);
            count++;
        }
        return count;
    }

    // ---- Đọc ---------------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SwapView> list() {
        CurrentUser me = users.getObject();
        Long myStaff = me.staffId().orElse(null);
        List<SwapView> out = new ArrayList<>();
        for (SimpleRecord<SwapDetail> row : records.swaps(allStatuses())) {
            SwapDetail d = row.content();
            boolean party = myStaff != null && (myStaff == d.giverStaffId() || myStaff == d.recipientStaffId());
            boolean approver = !party && canView(me, d);
            if (party || approver) {
                out.add(view(row, me, myStaff));
            }
        }
        out.sort(Comparator.comparingLong(SwapView::id).reversed());
        return out;
    }

    @Transactional(readOnly = true)
    public SwapView get(long id) {
        CurrentUser me = users.getObject();
        SimpleRecord<SwapDetail> row = records.swap(id);
        Long myStaff = me.staffId().orElse(null);
        boolean party = myStaff != null
                && (myStaff == row.content().giverStaffId() || myStaff == row.content().recipientStaffId());
        if (!party && !canView(me, row.content())) {
            throw new NotFoundException();
        }
        return view(row, me, myStaff);
    }

    /** FR-020: ca của người nhận thỏa FR-019, chỉ ngày, giờ, mẫu ca. */
    @Transactional(readOnly = true)
    public List<RecipientShift> recipientShifts(long shiftXId, long recipientId) {
        long giver = currentStaff();
        WorkShift x = shifts.shift(shiftXId, false);
        requirePresent(x, giver, true);
        requireRecipient(giver, recipientId, x);
        Instant now = time.now();
        List<RecipientShift> out = new ArrayList<>();
        for (WorkShift s : shifts.staffRange(recipientId, now, now.plus(Duration.ofDays(60)), true)) {
            if (!BLOCKING_SHIFT.contains(s.status())
                    || !s.startsAt().isAfter(now)
                    || !Objects.equals(s.scopeType(), x.scopeType())
                    || !Objects.equals(s.scopeId(), x.scopeId())
                    || s.id() == x.id()
                    || inShift(s, giver)
                    || records.hasOpenLock(s.id(), recipientId)) {
                continue;
            }
            out.add(new RecipientShift(s.id(), s.startsAt(), s.endsAt(), s.shiftTemplateId()));
        }
        return out;
    }

    /** FR-021: kết quả kiểm tra với dữ liệu hiện tại; mục (g) chỉ người duyệt, người nhận chỉ thấy phần của mình. */
    @Transactional(readOnly = true)
    public SwapChecks checks(long id) {
        CurrentUser me = users.getObject();
        SimpleRecord<SwapDetail> row = records.swap(id);
        SwapDetail d = row.content();
        Long myStaff = me.staffId().orElse(null);
        boolean approver = canView(me, d);
        boolean giver = myStaff != null && myStaff == d.giverStaffId();
        boolean recipient = myStaff != null && myStaff == d.recipientStaffId();
        if (!approver && !giver && !recipient) {
            throw new NotFoundException();
        }
        SwapChecks all = evaluate(d, approver);
        return approver ? all : all.withoutApproverOnly();
    }

    // ---- Kiểm tra FR-021 ---------------------------------------------------------------------------------------

    SwapChecks evaluate(SwapDetail d, boolean forApprover) {
        WorkShift x = shifts.shift(d.shiftXId(), false);
        WorkShift y = d.shiftYId() == null ? null : shifts.shift(d.shiftYId(), false);
        long a = d.giverStaffId();
        long b = d.recipientStaffId();
        Duration threshold = parameters.read(ParameterCode.CFG_M09_02).asDuration();
        List<Map<String, Object>> overlaps = new ArrayList<>();
        List<Map<String, Object>> hours = new ArrayList<>();
        List<WorkShift> bOthers = shifts.forStaff(b, y == null ? null : y.id(), false);
        List<WorkShift> aOthers = shifts.forStaff(a, x.id(), false);
        collect(b, x, bOthers, threshold, overlaps, hours);
        if (y != null) {
            collect(a, y, aOthers, threshold, overlaps, hours);
        }
        List<Map<String, Object>> notTransferable = new ArrayList<>();
        transferable(x, a, b, notTransferable);
        if (y != null) {
            transferable(y, b, a, notTransferable);
        }
        Map<String, Object> lead = new LinkedHashMap<>();
        lead.put("shiftX", leadAfter(x, a, b));
        if (y != null) {
            lead.put("shiftY", leadAfter(y, b, a));
        }
        Map<String, Object> monthly = null;
        if (forApprover) {
            monthly = monthly(a, b, x, y);
        }
        return new SwapChecks(overlaps, hours, notTransferable, lead, null, monthly, tripOverlaps(x, y, a, b));
    }

    private void collect(
            long staffId,
            WorkShift candidate,
            List<WorkShift> others,
            Duration threshold,
            List<Map<String, Object>> overlaps,
            List<Map<String, Object>> hours) {
        Interval slot = new Interval(candidate.startsAt(), candidate.endsAt());
        List<Interval> existing = others.stream()
                .map(s -> new Interval(s.startsAt(), s.endsAt()))
                .toList();
        if (SwapRules.anyOverlap(existing, slot)) {
            overlaps.add(Map.of("staffId", staffId, "shiftId", candidate.id()));
        } else if (ContinuousShiftChain.duration(existing, slot).compareTo(threshold) > 0) {
            hours.add(Map.of("staffId", staffId, "shiftId", candidate.id()));
        }
    }

    private void transferable(WorkShift shift, long from, long to, List<Map<String, Object>> out) {
        for (StaffAssignment a : assignments.inShift(shift.id(), false)) {
            if (a.staffId() != from || !"SCHEDULED".equals(a.status())) {
                continue;
            }
            AssignmentInput input = new AssignmentInput(
                    to,
                    a.targetType(),
                    a.targetId(),
                    a.taskTypeId(),
                    a.role(),
                    a.assignmentKind(),
                    a.assignedNurse(),
                    a.startsAt(),
                    a.endsAt());
            for (UnmetCondition c : assignmentPort.validate(shift.id(), input)) {
                // Người nhận chưa có tên trong ca lúc kiểm tra nên không đánh giá được mục này: chỉ báo lỗi về điều kiện.
                if (!c.description().contains("không có tên trong ca")) {
                    out.add(Map.of("assignmentId", a.id(), "shiftId", shift.id(), "reason", c.description()));
                    break;
                }
            }
        }
    }

    private Map<String, Object> leadAfter(WorkShift shift, long leaving, long arriving) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("before", shift.leadStaffId());
        if (!Objects.equals(shift.leadStaffId(), leaving)) {
            m.put("after", shift.leadStaffId());
            return m;
        }
        if (eligibleLead(arriving)) {
            m.put("after", arriving);
        } else {
            m.put("after", null);
            m.put("requiresChoice", true);
        }
        return m;
    }

    private Map<String, Object> monthly(long a, long b, WorkShift x, WorkShift y) {
        YearMonth month = YearMonth.from(x.startsAt().atZone(BusinessTime.ZONE));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("giver", monthlyOf(a, month, x, y));
        m.put("recipient", monthlyOf(b, month, y, x));
        return m;
    }

    private Map<String, Object> monthlyOf(long staffId, YearMonth month, WorkShift leaves, WorkShift joins) {
        List<WorkShift> all = shifts.forStaff(staffId, null, false);
        long before = SwapRules.hoursInMonth(intervals(all), month).toMinutes();
        List<Interval> after = new ArrayList<>(intervals(all.stream()
                .filter(s -> leaves == null || s.id() != leaves.id())
                .toList()));
        if (joins != null) {
            after.add(new Interval(joins.startsAt(), joins.endsAt()));
        }
        return Map.of("beforeMinutes", before, "afterMinutes", SwapRules.hoursInMonth(after, month).toMinutes());
    }

    private static List<Interval> intervals(Collection<WorkShift> list) {
        return list.stream().map(s -> new Interval(s.startsAt(), s.endsAt())).toList();
    }

    private List<Map<String, Object>> tripOverlaps(WorkShift x, WorkShift y, long a, long b) {
        List<Map<String, Object>> out = new ArrayList<>();
        trips.ifAvailable(port -> {
            tripsOf(port, x, a, out);
            if (y != null) {
                tripsOf(port, y, b, out);
            }
        });
        return out;
    }

    private void tripsOf(TripDutyPort port, WorkShift leaving, long staffId, List<Map<String, Object>> out) {
        if (leaving.scopeId() == null) {
            return;
        }
        port.inShift(leaving.scopeId(), leaving.startsAt(), leaving.endsAt()).stream()
                .filter(t -> t.staffIds().contains(staffId))
                .forEach(t -> out.add(Map.of("tripId", t.tripId(), "staffId", staffId)));
    }

    // ---- Điều kiện chung ---------------------------------------------------------------------------------------

    private void requireSwappable(WorkShift s) {
        if (!BLOCKING_SHIFT.contains(s.status()) || !s.startsAt().isAfter(time.now())) {
            throw invalidShift("Ca phải Đã công bố và chưa bắt đầu");
        }
    }

    private void requirePresent(WorkShift s, long staffId, boolean giver) {
        boolean present = shifts.members(s.id()).stream()
                .anyMatch(m -> m.staffId() == staffId && m.presentAt(time.now()));
        if (!present) {
            throw invalidShift(giver ? "Người nhường không có tên trong ca" : "Người nhận không có tên trong ca đã chọn");
        }
    }

    private boolean inShift(WorkShift s, long staffId) {
        return shifts.members(s.id()).stream().anyMatch(m -> m.staffId() == staffId);
    }

    /** FR-019: người nhận khác người nhường, Đang làm việc, tài khoản không Đã khóa, chưa có tên trong X, chung vai trò. */
    private void requireRecipient(long giver, long recipient, WorkShift x) {
        if (giver == recipient) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_RECIPIENT", "Người nhận phải khác người nhường");
        }
        var profile = staff.find(recipient).orElseThrow(NotFoundException::new);
        AccountView account = accounts.byStaff(recipient)
                .filter(acc -> acc.status() != AccountStatus.LOCKED)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT, "INVALID_RECIPIENT", "Người nhận chưa có tài khoản hoặc tài khoản đã khóa"));
        if (!"WORKING".equals(profile.status()) || inShift(x, recipient)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_RECIPIENT", "Người nhận không còn làm việc hoặc đã có tên trong ca");
        }
        Set<Role> mine = accounts.byStaff(giver).map(AccountView::roles).orElse(Set.of());
        if (mine.stream().noneMatch(account.roles()::contains)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ROLE_MISMATCH", "Người nhận không có vai trò hệ thống chung với người nhường");
        }
    }

    /** Dữ liệu hiện tại còn thỏa điều kiện cơ bản của FR-019 (dùng lúc đồng ý, duyệt). */
    private void ensureStillValid(SwapDetail d) {
        if (!stillValid(d)) {
            throw stateChanged();
        }
    }

    private boolean stillValid(SwapDetail d) {
        try {
            WorkShift x = shifts.shift(d.shiftXId(), false);
            if (!BLOCKING_SHIFT.contains(x.status()) || !x.startsAt().isAfter(time.now())) {
                return false;
            }
            if (!shifts.members(x.id()).stream().anyMatch(m -> m.staffId() == d.giverStaffId() && m.presentAt(time.now()))
                    || inShift(x, d.recipientStaffId())) {
                return false;
            }
            if (d.shiftYId() != null) {
                WorkShift y = shifts.shift(d.shiftYId(), false);
                if (!BLOCKING_SHIFT.contains(y.status())
                        || !y.startsAt().isAfter(time.now())
                        || shifts.members(y.id()).stream()
                                .noneMatch(m -> m.staffId() == d.recipientStaffId() && m.presentAt(time.now()))
                        || inShift(y, d.giverStaffId())) {
                    return false;
                }
            }
            var profile = staff.find(d.recipientStaffId());
            var giver = staff.find(d.giverStaffId());
            return profile.filter(p -> "WORKING".equals(p.status())).isPresent()
                    && giver.filter(p -> "WORKING".equals(p.status())).isPresent();
        } catch (NotFoundException e) {
            return false;
        }
    }

    /** FR-024 (c): người nhận làm người phụ trách nếu đủ điều kiện, không thì người duyệt chọn một người đủ điều kiện có tên trong ca. */
    private Long resolveLead(WorkShift shift, long leaving, long arriving, Long chosen, boolean first) {
        if (!Objects.equals(shift.leadStaffId(), leaving)) {
            return null;
        }
        if (chosen != null) {
            boolean ok = chosen == arriving
                    ? eligibleLead(arriving)
                    : eligibleLead(chosen)
                            && shifts.members(shift.id()).stream()
                                    .anyMatch(m -> m.staffId() == chosen && m.presentAt(time.now()))
                            && chosen != leaving;
            if (!ok) {
                throw new BusinessException(
                        HttpStatus.CONFLICT, "LEAD_REQUIRED", "Người phụ trách được chọn không đủ điều kiện");
            }
            return chosen;
        }
        if (eligibleLead(arriving)) {
            return arriving;
        }
        throw new BusinessException(
                HttpStatus.CONFLICT,
                "LEAD_REQUIRED",
                "Cần chọn người phụ trách mới cho ca " + (first ? "X" : "Y"),
                Map.of("shiftId", shift.id()));
    }

    private boolean eligibleLead(long staffId) {
        return staff.find(staffId).filter(p -> "WORKING".equals(p.status())).isPresent()
                && accounts.byStaff(staffId)
                        .filter(a -> a.status() != AccountStatus.LOCKED
                                && a.roles().stream().anyMatch(r -> r == Role.FLOOR_MANAGER || r == Role.NURSING_STAFF))
                        .isPresent();
    }

    // ---- Quyền -------------------------------------------------------------------------------------------------

    private long currentStaff() {
        return users.getObject()
                .staffId()
                .orElseThrow(() -> new PermissionDeniedException("Chỉ nhân viên dùng được chức năng này"));
    }

    private void requireIs(long me, long expected) {
        if (me != expected) {
            throw new PermissionDeniedException("Bạn không phải người được thao tác trên yêu cầu này");
        }
    }

    private boolean isParty(SwapDetail d, CurrentUser me) {
        return me.staffId().filter(s -> s == d.giverStaffId() || s == d.recipientStaffId()).isPresent();
    }

    /** FR-023: Trưởng tầng được giao của tầng/khu vực; ca toàn viện: Quản lý viện. */
    private boolean mayApprove(CurrentUser me, WorkShift x) {
        if (me.roles().contains(Role.NURSING_HOME_MANAGER)) {
            return true;
        }
        return x.scopeId() != null
                && me.roles().contains(Role.FLOOR_MANAGER)
                && permissions
                        .hasPermission(
                                me,
                                new PermissionKey(Function.SHIFT_SWAP_REQUEST, Operation.APPROVE),
                                new Target("M09.SHIFT", Long.toString(x.id()), null, x.scopeId()))
                        .allowed();
    }

    private void requireApprover(WorkShift x, SwapDetail d) {
        if (!mayApprove(users.getObject(), x)) {
            throw new PermissionDeniedException("Bạn không có quyền duyệt yêu cầu đổi ca này");
        }
    }

    private boolean canView(CurrentUser me, SwapDetail d) {
        try {
            return mayApprove(me, shifts.shift(d.shiftXId(), false));
        } catch (NotFoundException e) {
            return false;
        }
    }

    private SwapView view(SimpleRecord<SwapDetail> row, CurrentUser me, Long myStaff) {
        SwapDetail d = row.content();
        boolean giver = myStaff != null && myStaff == d.giverStaffId();
        boolean recipient = myStaff != null && myStaff == d.recipientStaffId();
        boolean approver = !giver && !recipient;
        List<String> commands = new ArrayList<>();
        String status = row.status();
        if (recipient && SwapStatus.AWAITING_RECIPIENT.equals(status)) {
            commands.add("accept");
            commands.add("decline");
        }
        if (recipient && SwapStatus.PENDING_APPROVAL.equals(status)) {
            commands.add("withdraw-acceptance");
        }
        if (giver && SwapStatus.isOpen(status)) {
            commands.add("cancel");
        }
        if ((approver || isParty(d, me)) && SwapStatus.PENDING_APPROVAL.equals(status) && canView(me, d)) {
            commands.add("approve");
            commands.add("reject");
        }
        return new SwapView(
                d.requestId(),
                status,
                d.kind(),
                d.giverStaffId(),
                d.recipientStaffId(),
                d.shiftXId(),
                d.shiftYId(),
                d.reason(),
                giver ? d.recipientDeclineReason() : null,
                d.selfApproved(),
                List.copyOf(commands));
    }

    // ---- Tiện ích ----------------------------------------------------------------------------------------------

    private String statusOf(long id) {
        return approvals.find(id).orElseThrow(NotFoundException::new).status();
    }

    private void requireStatus(long id, String expected) {
        if (!expected.equals(statusOf(id))) {
            throw stateChanged();
        }
    }

    private static BusinessException stateChanged() {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Yêu cầu đã đổi trạng thái; tải lại");
    }

    private static BusinessException invalidShift(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_SHIFT", message);
    }

    private static List<UnmetCondition> overlapConditions(SwapChecks checks) {
        return checks.overlaps().stream()
                .map(o -> new UnmetCondition(
                        "BR-M09-03", "Nhân viên #" + o.get("staffId") + " chồng giờ với ca #" + o.get("shiftId")))
                .toList();
    }

    private static Instant earliest(WorkShift x, WorkShift y) {
        return y != null && y.startsAt().isBefore(x.startsAt()) ? y.startsAt() : x.startsAt();
    }

    private void lockStaff(long... ids) {
        for (long id : new TreeSet<>(java.util.Arrays.stream(ids).boxed().toList())) {
            staff.lock(id);
        }
    }

    private Map<Long, WorkShift> lockShifts(long x, Long y) {
        Map<Long, WorkShift> out = new HashMap<>();
        TreeSet<Long> ids = new TreeSet<>();
        ids.add(x);
        if (y != null) {
            ids.add(y);
        }
        for (long id : ids) {
            out.put(id, shifts.shift(id, true));
        }
        return out;
    }

    private void releaseLocks(SwapDetail d) {
        records.release(d.shiftXId(), d.giverStaffId());
        if (d.shiftYId() != null) {
            records.release(d.shiftYId(), d.recipientStaffId());
        }
    }

    private static Set<String> allStatuses() {
        return Set.of(
                SwapStatus.AWAITING_RECIPIENT,
                SwapStatus.PENDING_APPROVAL,
                SwapStatus.APPLIED,
                SwapStatus.REJECTED,
                SwapStatus.CANCELLED,
                SwapStatus.RECIPIENT_DECLINED,
                SwapStatus.EXPIRED);
    }
}
