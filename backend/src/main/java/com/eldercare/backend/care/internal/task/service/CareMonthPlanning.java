package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.care.api.dto.CarePlanItemView;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.task.domain.CareShiftSources;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.domain.CareTaskDraft;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Q-453 (mở lại Q-439, FR-054): Trưởng tầng lập công việc cho cả tháng. "Chép từ kế hoạch" tạo việc Nháp cho mỗi mốc của
 * mục kế hoạch Hiệu lực trong các ca đã công bố của tầng; "Công bố công việc tháng" chuyển Nháp sang Chưa đến hạn. Chỉ chạy
 * khi Trưởng tầng bấm. Mốc đã có công việc (trùng căn cứ) hoặc không đạt kiểm tra của FR-017 thì bỏ qua và đếm.
 */
@Service
public class CareMonthPlanning {
    private final CareTaskService commands;
    private final CareTaskQueries queries;
    private final CareTaskValidator validator;
    private final CareTaskStore tasks;
    private final CurrentUser user;
    private final AuditLogApi audit;
    private final BusinessTime time;

    public CareMonthPlanning(
            CareTaskService commands,
            CareTaskQueries queries,
            CareTaskValidator validator,
            CareTaskStore tasks,
            CurrentUser user,
            AuditLogApi audit,
            BusinessTime time) {
        this.commands = commands;
        this.queries = queries;
        this.validator = validator;
        this.tasks = tasks;
        this.user = user;
        this.audit = audit;
        this.time = time;
    }

    /** Kết quả lệnh chép: số việc tạo, số mốc bỏ qua, số ca đã duyệt. */
    public record CopyResult(int created, int skipped, int shifts) {}

    public static Target floorTarget(long floorAreaId) {
        return new Target("M04.TASK", "0", null, floorAreaId);
    }

    private static Instant start(YearMonth month) {
        return month.atDay(1).atStartOfDay(BusinessTime.ZONE).toInstant();
    }

    private static Instant end(YearMonth month) {
        return start(month.plusMonths(1));
    }

    private void floorManager() {
        if (!user.roles().contains(Role.FLOOR_MANAGER))
            throw new PermissionDeniedException("Chỉ Trưởng tầng được lập công việc tháng");
    }

    @RequiresPermission(function = Function.SHIFT_TASK_PLANNING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CopyResult copyFromPlans(Target target, long floorAreaId, YearMonth month) {
        floorManager();
        Instant from = start(month), to = end(month), now = time.now();
        var shifts = commands.staff().publishedShifts(floorAreaId, from, to).stream()
                // Ca đã kết thúc không còn mốc nào để chép
                .filter(s -> s.endsAt().isAfter(now))
                .sorted(Comparator.comparing(ShiftStaffPort.ShiftView::startsAt))
                .toList();
        int created = 0, skipped = 0;
        for (var shift : shifts) {
            CareShiftSources sources = queries.sources(commands.shiftTarget(shift.id()), shift.id());
            for (var resident : sources.residents()) {
                Map<Long, CarePlanItemView> items = new java.util.HashMap<>();
                resident.planItems().forEach(item -> items.put(item.id(), item));
                for (var occurrence : resident.planOccurrences()) {
                    var item = items.get(occurrence.itemId());
                    // Chỉ chép mốc từ bây giờ trở đi: mốc đã qua mà công bố thì thành Quá hạn ngay
                    if (item == null
                            || occurrence.plannedAt().isBefore(from)
                            || !occurrence.plannedAt().isBefore(to)
                            || occurrence.plannedAt().isBefore(now)) continue;
                    var performer = performer(resident.assignments(), item.performerRole());
                    var draft = new CareTaskDraft(
                            resident.residentId(),
                            null,
                            null,
                            item.taskTypeId(),
                            SourceType.CARE_PLAN_ITEM,
                            item.id(),
                            null,
                            occurrence.plannedAt(),
                            item.windowBeforeMinutes(),
                            item.windowAfterMinutes(),
                            item.importance(),
                            performer.orElse(null),
                            performer.isEmpty());
                    var result = validator.validate(draft, shift, now, user.accountId(), null);
                    if (!result.codes().isEmpty()) {
                        skipped++;
                        continue;
                    }
                    try {
                        tasks.insert(asDraft(result.task()));
                        created++;
                    } catch (DuplicateKeyException duplicate) {
                        skipped++;
                    }
                }
            }
        }
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_TASK_MONTH_COPIED", false))
                .entity("M04.FLOOR", floorAreaId)
                .after(Map.of("month", month.toString(), "created", created, "skipped", skipped))
                .build());
        return new CopyResult(created, skipped, shifts.size());
    }

    /** Người phụ trách người cao tuổi trong ca có vai trò khớp mục (Q-214: Điều dưỡng làm được việc của Nhân viên chăm sóc). */
    private static Optional<Long> performer(List<ShiftStaffPort.CareAssignmentView> assignments, Role role) {
        return assignments.stream()
                .filter(a -> a.role() == role || (role == Role.CAREGIVER && a.role() == Role.NURSING_STAFF))
                .sorted(Comparator.comparing((ShiftStaffPort.CareAssignmentView a) -> !a.primary())
                        .thenComparing(a -> a.role() != role))
                .map(ShiftStaffPort.CareAssignmentView::staffId)
                .findFirst();
    }

    private static CareTask asDraft(CareTask t) {
        return new CareTask(
                t.id(),
                t.residentId(),
                t.locationType(),
                t.locationId(),
                t.taskTypeId(),
                t.basisType(),
                t.basisId(),
                t.planId(),
                t.originalTaskId(),
                t.shiftId(),
                t.floorAreaId(),
                t.plannedAt(),
                t.windowFrom(),
                t.windowTo(),
                t.importance(),
                t.performerRole(),
                t.performerId(),
                TaskStatus.DRAFT,
                t.data(),
                t.createdBy(),
                t.createdAt(),
                t.version(),
                t.awaitingRemeasurement(),
                t.carryover());
    }

    @RequiresPermission(function = Function.SHIFT_TASK_PLANNING, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<CareTask> drafts(Target target, long floorAreaId, YearMonth month) {
        return tasks.drafts(floorAreaId, start(month), end(month));
    }

    @RequiresPermission(function = Function.SHIFT_TASK_PLANNING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int publish(Target target, long floorAreaId, YearMonth month) {
        floorManager();
        int published = tasks.publishDrafts(floorAreaId, start(month), end(month));
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_TASK_MONTH_PUBLISHED", false))
                .entity("M04.FLOOR", floorAreaId)
                .after(Map.of("month", month.toString(), "published", published))
                .build());
        return published;
    }

    @RequiresPermission(function = Function.SHIFT_TASK_PLANNING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void discard(Target target, long id) {
        floorManager();
        var task = tasks.get(id, true);
        if (task.status() != TaskStatus.DRAFT)
            throw new BusinessException(HttpStatus.CONFLICT, "NOT_DRAFT", "Chỉ bỏ được công việc Nháp");
        if (!Objects.equals(target.floorAreaId(), task.floorAreaId())) throw new NotFoundException();
        tasks.discardDraft(id);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_TASK_DRAFT_DISCARDED", false))
                .entity("M04.TASK", id)
                .build());
    }
}
