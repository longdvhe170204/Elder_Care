package com.eldercare.backend.care.internal.qualitycheck.service;

import static com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds.*;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.QualityCheckFailedNotice;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.care.internal.qualitycheck.domain.CompletedTask;
import com.eldercare.backend.care.internal.qualitycheck.persistence.QualityCheckStore;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T024, T025 (FR-059 → FR-062, FR-064, FR-066; BR-M04-23; research L-10): người kiểm tra tự chọn công việc, hệ thống
 * chỉ kiểm tỷ lệ CFG-M04-11, không chọn, gợi ý hay xếp hạng (Q-307). Người kiểm tra là Trưởng tầng được giao của tầng;
 * Người phụ trách ca của tầng khi ca thuộc ngoại lệ không có Trưởng tầng ({@code DutyApi.shiftHasNoFloorManager}, Q-262).
 * Nhắc, quá hạn và ca không có danh sách làm ở lượt sau.
 */
@Service
public class QualityCheckService {
    public static final String ENTITY = "M04.QUALITY_CHECK";
    private static final String CARE_RESULT = "M04.CARE_RESULT";

    private final RecordEventApi records;
    private final SimpleRecordApi lists;
    private final QualityCheckStore store;
    private final ObjectProvider<ShiftStaffPort> shifts;
    private final ObjectProvider<DutyApi> duties;
    private final AccountApi accounts;
    private final CorrectionApi corrections;
    private final ParameterApi parameters;
    private final CareTaskService tasks;
    private final CurrentUser user;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;

    public QualityCheckService(
            RecordEventApi records,
            SimpleRecordApi lists,
            QualityCheckStore store,
            ObjectProvider<ShiftStaffPort> shifts,
            ObjectProvider<DutyApi> duties,
            AccountApi accounts,
            CorrectionApi corrections,
            ParameterApi parameters,
            CareTaskService tasks,
            CurrentUser user,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events) {
        this.records = records;
        this.lists = lists;
        this.store = store;
        this.shifts = shifts;
        this.duties = duties;
        this.accounts = accounts;
        this.corrections = corrections;
        this.parameters = parameters;
        this.tasks = tasks;
        this.user = user;
        this.time = time;
        this.audit = audit;
        this.events = events;
    }

    public static Target target(long floorAreaId) {
        return new Target(ENTITY, null, null, floorAreaId);
    }

    public Target itemTarget(long itemId) {
        var item = records.require(ITEMS, itemId).content();
        return new Target(ENTITY, Long.toString(item.checklistId()), null, item.floorAreaId());
    }

    /** FR-059: lập danh sách (đủ tỷ lệ tại lúc lập) hoặc thêm mục vào danh sách của ca, tới hết ca. */
    @RequiresPermission(function = Function.QUALITY_CHECK, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long select(Target target, long shiftId, long floorAreaId, List<Long> taskIds) {
        if (taskIds == null
                || taskIds.isEmpty()
                || taskIds.stream().anyMatch(Objects::isNull)
                || new HashSet<>(taskIds).size() != taskIds.size())
            throw new InvalidValueException("Cần danh sách công việc không trùng");
        var shift = shift(shiftId, floorAreaId);
        var now = time.now();
        requireWithinShift(shift, now);
        var context = inspection(shift, now);
        var eligible = eligible(shift, context);
        var key = shiftId + ":" + floorAreaId;
        var existing = lists.findByUniqueKey(CHECKLISTS, key);
        for (long id : taskIds) {
            var task = eligible.get(id);
            if (task == null || records.findByUniqueKey(ITEMS, "QC_TASK:" + id).isPresent())
                throw conflict("TASK_NOT_ELIGIBLE", "Công việc không đủ điều kiện hoặc đã được chọn");
        }
        long checklistId;
        if (existing.isEmpty()) {
            int minimum = minimumItems(
                    eligible.size(), parameters.read(ParameterCode.CFG_M04_11).asPercent());
            if (taskIds.size() < minimum)
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "SAMPLE_RATIO_NOT_MET",
                        "Cần ít nhất " + minimum + " công việc theo tỷ lệ kiểm tra",
                        Map.of(
                                "unmetConditions",
                                List.of(new UnmetCondition(
                                        "SAMPLE_RATIO_NOT_MET",
                                        "Cần ít nhất " + minimum + " trên " + eligible.size()
                                                + " công việc đủ điều kiện"))));
            checklistId = lists.create(
                            CHECKLISTS,
                            new NewSimpleRecord<>(
                                    null,
                                    null,
                                    "OPEN",
                                    new Checklist(
                                            shiftId,
                                            floorAreaId,
                                            user.accountId(),
                                            context.role(),
                                            now,
                                            eligible.size()),
                                    null,
                                    null,
                                    key,
                                    user.accountId(),
                                    false))
                    .id();
        } else
            checklistId = lists.lockForUpdate(CHECKLISTS, existing.get().id()).id();
        for (long id : taskIds) {
            var task = eligible.get(id);
            records.add(
                    ITEMS,
                    new EntityRef("M04.QUALITY_CHECKLIST", checklistId),
                    new NewRecordEvent<>(
                            new Item(
                                    checklistId,
                                    shiftId,
                                    floorAreaId,
                                    id,
                                    task.recordedBy(),
                                    task.performerStaffId(),
                                    PENDING,
                                    null,
                                    List.of(),
                                    null,
                                    null,
                                    null),
                            null,
                            user.accountId(),
                            false,
                            now,
                            null,
                            null,
                            floorAreaId,
                            "QC_TASK:" + id));
        }
        log(checklistId, existing.isEmpty() ? "QUALITY_CHECKLIST_CREATED" : "QUALITY_CHECK_ITEMS_ADDED", taskIds);
        return checklistId;
    }

    /**
     * GET danh sách của ca (FR-059, FR-060; schema duyệt 07/11/2026): công việc đủ điều kiện chưa chọn, số mục tối
     * thiểu, danh sách và mục. Không xếp hạng, không gợi ý (Q-306, Q-307).
     */
    public record ShiftOverview(
            long shiftId,
            long floorAreaId,
            int minimumItems,
            int eligibleTaskCount,
            Optional<SimpleRecord<Checklist>> checklist,
            List<CompletedTask> eligibleTasks,
            List<OverviewItem> items) {}

    public record OverviewItem(
            long id, long taskId, String status, String note, String inspectorRole, Instant recordedAt) {
        static OverviewItem of(RecordEvent<Item> row, CurrentValue value) {
            var item = row.content();
            String result = item.status();
            String note = item.note();
            if (value != null && value.content() instanceof Map<?, ?> correction) {
                result = (String) correction.get("result");
                note = (String) correction.get("note");
            }
            return new OverviewItem(row.id(), item.taskId(), result, note, item.inspectorRole(), item.recordedAt());
        }
    }

    /** Chỉ người kiểm tra của ca (FR-066) và Quản lý viện; người thực hiện không xem (không báo khi được chọn). */
    @RequiresPermission(function = Function.QUALITY_CHECK, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public ShiftOverview overview(Target target, long shiftId, long floorAreaId) {
        var shift = shift(shiftId, floorAreaId);
        var now = time.now();
        Inspection context;
        if (user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            var duty = duties();
            context = new Inspection(
                    null,
                    duty.assignedFloorManager(floorAreaId, now).orElse(null),
                    duty.shiftHasNoFloorManager(shiftId));
        } else context = inspection(shift, now);
        var eligible = eligible(shift, context);
        var checklist = lists.findByUniqueKey(CHECKLISTS, shiftId + ":" + floorAreaId);
        var items = checklist
                .map(l -> records.list(ITEMS, new EntityRef("M04.QUALITY_CHECKLIST", l.id())))
                .orElse(List.of());
        var selected = items.stream().map(i -> i.content().taskId()).collect(java.util.stream.Collectors.toSet());
        var amended = corrections.current(
                ITEMS.code(),
                items.stream()
                        .filter(i -> i.confirmedAt() != null)
                        .map(RecordEvent::id)
                        .toList());
        return new ShiftOverview(
                shiftId,
                floorAreaId,
                minimumItems(
                        eligible.size(),
                        parameters.read(ParameterCode.CFG_M04_11).asPercent()),
                eligible.size(),
                checklist,
                eligible.values().stream()
                        .filter(t -> !selected.contains(t.id()))
                        .toList(),
                items.stream().map(i -> OverviewItem.of(i, amended.get(i.id()))).toList());
    }

    public record Rework(Instant plannedAt, Long performerId, boolean sharedOnFloor) {}

    /** FR-061, FR-062, FR-064: kết quả nhóm 3; Không đạt lập công việc làm lại của 005 trong cùng transaction. */
    @RequiresPermission(function = Function.QUALITY_CHECK, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void record(Target target, long itemId, String result, String note, List<Long> photos, Rework rework) {
        validateResult(result, note);
        var row = records.require(ITEMS, itemId);
        var item = row.content();
        if (row.confirmedAt() != null || !PENDING.equals(item.status()))
            throw conflict("STATE_CHANGED", "Mục đã có kết quả");
        var shift = shift(item.shiftId(), item.floorAreaId());
        var now = time.now();
        requireWithinShift(shift, now);
        var context = requireResultInspector(shift, item, now);
        if (FAIL.equals(result)) requireReworkTime(shift, now, rework);
        lists.lockForUpdate(CHECKLISTS, item.checklistId());
        records.updateDraft(ITEMS, itemId, item.result(result, note, photos, user.accountId(), context.role(), now));
        if (FAIL.equals(result)) createRework(itemId, item, rework, now, "QUALITY_CHECK_FAILED:" + itemId);
        records.confirm(ITEMS, itemId, now);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("QUALITY_CHECK_RESULT_RECORDED", false))
                .entity("M04.QUALITY_CHECK_ITEM", itemId)
                .before(Map.of("status", PENDING))
                .after(Map.of("status", result, "inspectorRole", context.role()))
                .build());
    }

    static void validateResult(String result, String note) {
        if (!Set.of(PASS, FAIL, NOT_CHECKABLE).contains(result == null ? "" : result))
            throw new InvalidValueException("Kết quả phải là Đạt, Không đạt hoặc Không kiểm tra được");
        if (!PASS.equals(result) && (note == null || note.isBlank()))
            throw new InvalidValueException("Không đạt hoặc Không kiểm tra được cần ghi chú");
    }

    static void requireRework(Rework rework) {
        if (rework == null || rework.plannedAt() == null)
            throw conflict("REWORK_TASK_REQUIRED", "Ghi Không đạt cần kèm công việc làm lại");
    }

    @RequiresPermission(function = Function.QUALITY_CHECK, operation = Operation.VIEW)
    public void requireHistoryView(Target target, Item item) {
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER))
            inspection(shift(item.shiftId(), item.floorAreaId()), time.now());
    }

    @RequiresPermission(function = Function.QUALITY_CHECK, operation = Operation.UPDATE)
    public void requireAmendment(Target target, Item item, Instant now) {
        var shift = shift(item.shiftId(), item.floorAreaId());
        requireWithinShift(shift, now);
        requireResultInspector(shift, item, now);
    }

    void reworkAfterAmendment(long itemId, Item item, Rework rework, Instant now, long correctionId) {
        requireReworkTime(shift(item.shiftId(), item.floorAreaId()), now, rework);
        createRework(itemId, item, rework, now, "QUALITY_CHECK_FAILED:" + itemId + ":C:" + correctionId);
    }

    private Inspection requireResultInspector(ShiftStaffPort.ShiftView shift, Item item, Instant now) {
        var context = inspection(shift, now);
        if (SHIFT_LEAD.equals(context.role())
                && item.performerAccountId() != null
                && item.performerAccountId() == user.accountId())
            throw conflict("SELF_CHECK_NOT_ALLOWED", "Người phụ trách ca không ghi kết quả công việc của chính mình");
        return context;
    }

    private static void requireReworkTime(ShiftStaffPort.ShiftView shift, Instant now, Rework rework) {
        requireRework(rework);
        if (rework.plannedAt().isBefore(now) || !rework.plannedAt().isBefore(shift.endsAt()))
            throw new InvalidValueException("Công việc làm lại phải dự kiến trong ca của danh sách");
    }

    private void createRework(long itemId, Item item, Rework rework, Instant now, String eventKey) {
        tasks.rework(
                tasks.target(item.taskId()),
                item.taskId(),
                itemId,
                rework.plannedAt(),
                rework.performerId(),
                rework.sharedOnFloor());
        if (item.performerStaffId() != null)
            events.publishEvent(new QualityCheckFailedNotice(
                    itemId,
                    item.taskId(),
                    item.performerStaffId(),
                    new CareEventContext(
                            ITEMS.code(),
                            itemId,
                            eventKey,
                            now,
                            Optional.of(item.floorAreaId()),
                            List.of(new RecipientGroup.Staff(item.performerStaffId(), "TASK_PERFORMER")))));
    }

    /** Id nhân viên của Trưởng tầng được giao; {@code noFloorManager} là ngoại lệ FR-066 của ca (DutyApi, Q-262). */
    private record Inspection(String role, Long floorManager, boolean noFloorManager) {}

    /**
     * Số công việc đủ điều kiện của ca và người kiểm tra để nhắc (FR-059a, FR-063); không theo người dùng hiện tại.
     * {@code noInspector}: ca không có Trưởng tầng được giao trong ca mà cũng không có Người phụ trách ca.
     */
    public record ShiftStatus(int eligible, List<Long> inspectorStaffIds, boolean noInspector) {}

    @Transactional(readOnly = true)
    public ShiftStatus status(ShiftStaffPort.ShiftView shift, Instant at) {
        var duty = duties();
        Long manager = duty.assignedFloorManager(shift.floorAreaId(), at).orElse(null);
        boolean noManager = duty.shiftHasNoFloorManager(shift.id());
        var inspectors = new ArrayList<Long>();
        if (manager != null) inspectors.add(manager);
        Long lead = noManager ? duty.shiftLead(shift.floorAreaId(), at).orElse(null) : null;
        if (lead != null && !lead.equals(manager)) inspectors.add(lead);
        int eligible = eligible(shift, new Inspection(null, manager, noManager)).size();
        return new ShiftStatus(eligible, List.copyOf(inspectors), noManager && lead == null);
    }

    /** FR-066: Trưởng tầng được giao; hoặc Người phụ trách ca hiện hành khi ca thuộc ngoại lệ không có Trưởng tầng. */
    private Inspection inspection(ShiftStaffPort.ShiftView shift, Instant now) {
        var duty = duties();
        Long me = user.staffId().orElse(null);
        Long manager = duty.assignedFloorManager(shift.floorAreaId(), now).orElse(null);
        boolean noManager = duty.shiftHasNoFloorManager(shift.id());
        if (me != null && me.equals(manager)) return new Inspection(FLOOR_MANAGER, manager, noManager);
        if (me != null
                && noManager
                && duty.shiftLead(shift.floorAreaId(), now).filter(me::equals).isPresent())
            return new Inspection(SHIFT_LEAD, manager, true);
        throw new PermissionDeniedException("Chỉ người kiểm tra của ca được lập danh sách và ghi kết quả");
    }

    private Long staffOf(long accountId) {
        return accounts.byId(accountId).map(AccountView::staffId).orElse(null);
    }

    private DutyApi duties() {
        var source = duties.getIfAvailable();
        if (source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", "Chưa có nguồn nhiệm vụ 008");
        return source;
    }

    /**
     * FR-060: công việc hoàn thành trong ca của tầng, trừ công việc của Trưởng tầng được giao, của Người phụ trách ca (xét
     * tại lúc hoàn thành, Q-270) khi Trưởng tầng không có tên trong ca, công việc làm lại của ca và kết quả đã Hủy ghi nhận.
     */
    /**
     * 016 FR-037 (Q-168, Q-254): công việc hoàn thành trong {@code [from, to)} không vào mẫu vì do Trưởng tầng được giao
     * (tại lúc thực hiện), hoặc Người phụ trách ca của ca không có Trưởng tầng được giao, tự thực hiện; bỏ công việc làm lại
     * và kết quả đã Hủy ghi nhận như {@link #eligible}. Chỉ đọc; thiếu nguồn ca, nhiệm vụ thì rỗng.
     */
    @Transactional(readOnly = true)
    public List<com.eldercare.backend.care.api.dto.OutOfCheckScopeView> outOfCheckScope(
            com.eldercare.backend.administration.account.api.dto.ScopeFilter scope, Instant from, Instant to) {
        var shiftSource = shifts.getIfAvailable();
        var duty = duties.getIfAvailable();
        if (shiftSource == null || duty == null) return List.of();
        var out = new ArrayList<com.eldercare.backend.care.api.dto.OutOfCheckScopeView>();
        for (long[] pair : store.shiftsCompletedIn(from, to)) {
            long floor = pair[1];
            if (!scope.facilityWide() && !scope.floorAreaIds().contains(floor)) continue;
            var shift = shiftSource.shift(pair[0]).orElse(null);
            if (shift == null) continue;
            boolean noManager = duty.shiftHasNoFloorManager(shift.id());
            var candidates = store.completed(shift.id(), floor, from, to);
            var current = corrections.current(
                    CARE_RESULT, candidates.stream().map(CompletedTask::id).toList());
            for (var t : candidates) {
                if ("REWORK".equals(t.basisType())) continue;
                var value = current.get(t.id());
                if (value != null && value.voided()) continue;
                Long performer = staffOf(t.recordedBy());
                if (performer == null) continue;
                String role = null;
                if (duty.assignedFloorManager(floor, t.performedAt())
                        .filter(performer::equals)
                        .isPresent()) role = FLOOR_MANAGER;
                else if (noManager
                        && duty.shiftLead(floor, t.performedAt())
                                .filter(performer::equals)
                                .isPresent()) role = SHIFT_LEAD;
                if (role != null)
                    out.add(new com.eldercare.backend.care.api.dto.OutOfCheckScopeView(
                            t.id(), shift.id(), floor, performer, role, t.performedAt()));
            }
        }
        return out;
    }

    private Map<Long, CompletedTask> eligible(ShiftStaffPort.ShiftView shift, Inspection context) {
        var candidates = store.completed(shift.id(), shift.floorAreaId(), shift.startsAt(), shift.endsAt());
        var voided = corrections.current(
                CARE_RESULT, candidates.stream().map(CompletedTask::id).toList());
        var result = new LinkedHashMap<Long, CompletedTask>();
        for (var t : candidates) {
            if ("REWORK".equals(t.basisType())) continue;
            Long performer = staffOf(t.recordedBy());
            if (performer != null && performer.equals(context.floorManager())) continue;
            if (performer != null
                    && context.noFloorManager()
                    && duties().shiftLead(shift.floorAreaId(), t.performedAt())
                            .filter(performer::equals)
                            .isPresent()) continue;
            var current = voided.get(t.id());
            if (current != null && current.voided()) continue;
            result.put(t.id(), t);
        }
        return result;
    }

    private ShiftStaffPort.ShiftView shift(long shiftId, long floorAreaId) {
        var shift = shifts().shift(shiftId).orElseThrow(NotFoundException::new);
        if (shift.floorAreaId() != floorAreaId) throw new NotFoundException();
        return shift;
    }

    private static void requireWithinShift(ShiftStaffPort.ShiftView shift, Instant now) {
        if (now.isBefore(shift.startsAt()) || !now.isBefore(shift.endsAt()))
            throw conflict("OUTSIDE_SHIFT_TIME", "Chỉ lập danh sách và ghi kết quả trong thời gian của ca");
    }

    private ShiftStaffPort shifts() {
        var source = shifts.getIfAvailable();
        if (source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", "Chưa có nguồn ca 008");
        return source;
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private void log(long checklistId, String action, List<Long> taskIds) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, false))
                .entity("M04.QUALITY_CHECKLIST", checklistId)
                .before(Map.of())
                .after(Map.of("careTaskIds", taskIds))
                .build());
    }
}
