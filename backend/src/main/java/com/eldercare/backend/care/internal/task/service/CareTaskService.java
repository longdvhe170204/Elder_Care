package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T023: kiểm toàn bộ rồi ghi, khóa cư dân tăng dần; UNIQUE là chốt chống lệnh đồng thời. */
@Service
public class CareTaskService {
    public static final HistoryTrack<CareTaskHandling> HISTORY =
            new HistoryTrack<>("M04.TASK", "HANDLING", CareTaskHandling.class);
    private final CareTaskStore tasks;
    private final CareLockStore locks;
    private final CareTaskValidator validator;
    private final ObjectProvider<ShiftStaffPort> staffing;
    private final ObjectProvider<QualityCheckPort> quality;
    private final CurrentUser user;
    private final PermissionApi permissions;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final StatusHistoryApi history;
    private final CareTaskNotices notices;
    private final BedAllocationApi beds;

    public CareTaskService(
            CareTaskStore tasks,
            CareLockStore locks,
            CareTaskValidator validator,
            ObjectProvider<ShiftStaffPort> staffing,
            ObjectProvider<QualityCheckPort> quality,
            CurrentUser user,
            PermissionApi permissions,
            BusinessTime time,
            ParameterApi parameters,
            AuditLogApi audit,
            StatusHistoryApi history,
            CareTaskNotices notices,
            BedAllocationApi beds) {
        this.tasks = tasks;
        this.locks = locks;
        this.validator = validator;
        this.staffing = staffing;
        this.quality = quality;
        this.user = user;
        this.permissions = permissions;
        this.time = time;
        this.parameters = parameters;
        this.audit = audit;
        this.history = history;
        this.notices = notices;
        this.beds = beds;
    }

    public ShiftStaffPort staff() {
        var p = staffing.getIfAvailable();
        if (p == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", "Chưa có nguồn ca/phân công chính thức");
        return p;
    }

    public ShiftStaffPort.ShiftView shift(long id) {
        return staff().shift(id).orElseThrow(NotFoundException::new);
    }

    public Target shiftTarget(long id) {
        var s = shift(id);
        return new Target("M04.SHIFT", Long.toString(id), null, s.floorAreaId());
    }

    public static Target target(CareTask t) {
        return new Target("M04.TASK", Long.toString(t.id()), t.residentId(), t.floorAreaId());
    }

    public Target target(long id) {
        return target(tasks.get(id, false));
    }

    private void permission(Function function, Operation op, Target target) {
        if (!permissions
                .hasPermission(user, new PermissionKey(function, op), target)
                .allowed()) throw new PermissionDeniedException("Không có quyền với đối tượng này");
    }

    private void lead(ShiftStaffPort.ShiftView shift, Instant now) {
        if (!shift.published()) throw new BusinessException(HttpStatus.CONFLICT, "OUTSIDE_SHIFT", "Ca chưa công bố");
        if (user.roles().contains(Role.FLOOR_MANAGER)) return;
        if (user.staffId().isEmpty() || !staff().shiftLead(shift.id()).equals(user.staffId()))
            throw new PermissionDeniedException("Chỉ Người phụ trách ca hoặc Trưởng tầng được lập công việc của ca");
        if (now.isBefore(shift.startsAt()
                        .minus(parameters.read(ParameterCode.CFG_M04_01).asDuration()))
                || !now.isBefore(shift.endsAt()))
            throw new PermissionDeniedException("Chưa tới thời gian lập công việc hoặc ca đã kết thúc");
    }

    @RequiresPermission(function = Function.SHIFT_TASK_PLANNING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<CareTask> plan(Target target, long shiftId, List<CareTaskDraft> rows) {
        var s = shift(shiftId);
        lead(s, time.now());
        return saveRows(rows, s, null, Function.SHIFT_TASK_PLANNING);
    }

    @RequiresPermission(function = Function.TASK_CHECKLIST, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CareTask adHoc(Target target, CareTaskDraft row) {
        if (Collections.disjoint(user.roles(), Set.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF)))
            throw new PermissionDeniedException("Chỉ Trưởng tầng hoặc Điều dưỡng tạo công việc phát sinh");
        if (row.basisType() != SourceType.AD_HOC)
            throw new InvalidValueException("Công việc phát sinh phải có căn cứ AD_HOC");
        var s = staff().shiftContaining(target.floorAreaId(), row.plannedAt()).orElseThrow(NotFoundException::new);
        return saveRows(List.of(row), s, null, Function.TASK_CHECKLIST).getFirst();
    }

    @RequiresPermission(function = Function.QUALITY_CHECK, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CareTask rework(
            Target target, long originalId, long checkId, Instant plannedAt, Long performer, boolean shared) {
        var original = tasks.get(originalId, false);
        var provider = quality.getIfAvailable();
        var check = provider == null
                ? Optional.<QualityCheckPort.FailedCheckView>empty()
                : provider.failed(checkId, originalId);
        if (check.isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_BASIS", "Không có kết quả kiểm tra Không đạt cho công việc này");
        var s = shift(check.get().shiftId());
        var row = new CareTaskDraft(
                original.residentId(),
                original.locationType(),
                original.locationId(),
                original.data().publicTaskTypeId(),
                SourceType.REWORK,
                checkId,
                original.data().content(),
                plannedAt,
                Math.toIntExact(Duration.between(original.windowFrom(), original.plannedAt())
                        .toMinutes()),
                Math.toIntExact(Duration.between(original.plannedAt(), original.windowTo())
                        .toMinutes()),
                original.importance(),
                performer,
                shared);
        return saveRows(List.of(row), s, originalId, Function.QUALITY_CHECK).getFirst();
    }

    private List<CareTask> saveRows(
            List<CareTaskDraft> rows, ShiftStaffPort.ShiftView s, Long originalId, Function function) {
        locks.lock(rows.stream()
                .map(CareTaskDraft::residentId)
                .filter(Objects::nonNull)
                .toList());
        // Đọc lại ca sau khóa người; không giả rằng hàm đọc nguồn đồng nghĩa khóa dữ liệu 008.
        s = shift(s.id());
        var errors = new ArrayList<Map<String, Object>>();
        var validated = new ArrayList<CareTask>();
        var seen = new HashSet<String>();
        Instant now = time.now();
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            if (row.residentId() != null) {
                var allocation = beds.openAt(row.residentId(), row.plannedAt());
                boolean belongsToShift = allocation.isPresent()
                        ? allocation.get().floorAreaId() == s.floorAreaId()
                        : !staff().assignments(row.residentId(), s.id()).isEmpty();
                if (!belongsToShift)
                    throw new PermissionDeniedException("Người cao tuổi không thuộc phạm vi tầng/khu của ca");
            }
            permission(function, Operation.CREATE, new Target("M04.TASK", "0", row.residentId(), s.floorAreaId()));
            CareTaskValidation result;
            try {
                result = validator.validate(row, s, now, user.accountId(), originalId);
            } catch (NotFoundException missing) {
                errors.add(Map.of("row", i + 1, "codes", List.of("INVALID_BASIS")));
                continue;
            }
            var codes = new ArrayList<>(result.codes());
            var t = result.task();
            boolean special = t.basisType() == SourceType.SHIFT_START_CHECK || t.basisType() == SourceType.SUPERVISION;
            if (t.basisType() != SourceType.AD_HOC && t.basisType() != SourceType.REWORK) {
                String key = t.basisType() + ":"
                        + (special
                                ? t.shiftId() + ":" + t.performerId()
                                : t.basisId() + ":" + t.residentId() + ":" + t.plannedAt());
                if (!seen.add(key)) codes.add(special ? "SHIFT_CHECK_ALREADY_EXISTS" : "DUPLICATE_BASIS");
            }
            if (!codes.isEmpty()) errors.add(Map.of("row", i + 1, "codes", List.copyOf(new LinkedHashSet<>(codes))));
            validated.add(t);
        }
        if (!errors.isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "UNMET_CONDITIONS",
                    "Có dòng chưa đủ điều kiện, chưa lưu lô công việc",
                    Map.of("rowErrors", errors));
        var result = new ArrayList<CareTask>();
        for (int i = 0; i < validated.size(); i++) {
            CareTask saved;
            try {
                saved = tasks.insert(validated.get(i));
            } catch (DuplicateKeyException e) {
                var type = validated.get(i).basisType();
                String code = type == SourceType.SHIFT_START_CHECK || type == SourceType.SUPERVISION
                        ? "SHIFT_CHECK_ALREADY_EXISTS"
                        : "DUPLICATE_BASIS";
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_BASIS",
                        "Lô trùng công việc với lệnh đồng thời",
                        Map.of("rowErrors", List.of(Map.of("row", i + 1, "codes", List.of(code)))));
            }
            record(null, saved, "CREATED", null, false);
            if (saved.status() == TaskStatus.OVERDUE) notices.overdue(saved, now);
            result.add(saved);
        }
        return List.copyOf(result);
    }

    @RequiresPermission(function = Function.SHIFT_TASK_PLANNING, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(Target target, long id, Reason reason) {
        if (!user.roles().contains(Role.FLOOR_MANAGER))
            throw new PermissionDeniedException("Chỉ Trưởng tầng được hủy công việc");
        var before = locked(id);
        if (before.status() == TaskStatus.NOT_YET_DUE && !time.now().isBefore(before.windowFrom()))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "TASK_CLOSED", "Công việc đã tới đầu khung, không được hủy");
        var next = CareTaskTransitions.next(before.status(), "CANCEL", before.importance())
                .orElseThrow(() ->
                        new BusinessException(HttpStatus.CONFLICT, "TASK_CLOSED", "Chỉ hủy công việc Chưa đến hạn"));
        var after = tasks.transition(before, next, time.now(), reason.value(), "MANUAL_CANCEL");
        record(before, after, "CANCELLED", reason, false);
    }

    public CareTask locked(long id) {
        var t = tasks.get(id, false);
        if (t.residentId() != null) locks.lock(List.of(t.residentId()));
        return tasks.get(id, true);
    }

    public void record(CareTask before, CareTask after, String command, Reason reason, boolean system) {
        history.add(
                HISTORY,
                after.id(),
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        before == null ? null : before.status().name(),
                        after.status().name(),
                        null,
                        time.now(),
                        command,
                        system ? null : user.accountId(),
                        system,
                        reason,
                        after.basisType().name(),
                        after.basisId(),
                        new CareTaskHandling(after.data().content()),
                        after.floorAreaId(),
                        null));
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_TASK_" + command, reason != null))
                .entity("M04.TASK", after.id())
                .before(before == null ? Map.of() : Map.of("status", before.status()))
                .after(Map.of("status", after.status(), "content", after.data().content()))
                .reason(reason)
                .build());
    }
}
