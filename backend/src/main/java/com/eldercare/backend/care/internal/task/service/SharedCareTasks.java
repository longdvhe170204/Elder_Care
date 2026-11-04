package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.internal.catalog.domain.CareCatalogKinds;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T056: công việc thành chung, gán lại theo phân công, phân lại (FR-024, FR-031, FR-032, BR-M04-13, 28). */
@Service
public class SharedCareTasks {
    private static final Set<TaskStatus> OPEN = Set.of(TaskStatus.NOT_YET_DUE, TaskStatus.DUE, TaskStatus.OVERDUE);

    private final CareTaskStore tasks;
    private final CareTaskService commands;
    private final CareTaskNotices notices;
    private final CareLockStore locks;
    private final CareCatalogStore catalogs;
    private final ReminderMarkers markers;
    private final BusinessTime time;

    public SharedCareTasks(
            CareTaskStore tasks,
            CareTaskService commands,
            CareTaskNotices notices,
            CareLockStore locks,
            CareCatalogStore catalogs,
            ReminderMarkers markers,
            BusinessTime time) {
        this.tasks = tasks;
        this.commands = commands;
        this.notices = notices;
        this.locks = locks;
        this.catalogs = catalogs;
        this.markers = markers;
        this.time = time;
    }

    /** Vắng ca, nghỉ có duyệt (Q-91): chỉ công việc chưa đóng của người đó trong các ca nêu. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void shareInShifts(long staffId, Collection<Long> shiftIds, String command) {
        for (long shiftId : new TreeSet<>(shiftIds))
            share(
                    tasks.inShift(shiftId).stream()
                            .filter(t -> Objects.equals(t.performerId(), staffId))
                            .toList(),
                    command);
    }

    /** Nghỉ việc: ca hiện tại và các ca tương lai (ca kết thúc sau thời điểm nghỉ). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void shareFrom(long staffId, Instant at) {
        share(
                tasks.openOfPerformer(staffId).stream()
                        .filter(t -> commands.shift(t.shiftId()).endsAt().isAfter(at))
                        .toList(),
                "SHARED_ON_RESIGNATION");
    }

    /** Đổi phân công của một người cao tuổi giữa ca (FR-024). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reassignResident(long residentId, long shiftId) {
        reassign(tasks.inShift(shiftId).stream()
                .filter(t -> Objects.equals(t.residentId(), residentId))
                .toList());
    }

    /** Kết thúc phân công: công việc mà người thực hiện không còn được phân công thì gán lại hoặc thành chung. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reassignShift(long shiftId) {
        reassign(tasks.inShift(shiftId).stream()
                .filter(t -> t.residentId() != null && t.performerId() != null)
                .toList());
    }

    /** BR-M03-04: nghe sau commit lệnh chuyển giường của 003 (research C-10). */
    @ApplicationModuleListener
    void bedTransferred(BedTransferred event) {
        onBedTransferred(event);
    }

    /** Chuyển giường sang khu khác (FR-032 câu cuối): theo phân công của ca khu mới; chạy lại không đổi thêm. */
    @Transactional
    public void onBedTransferred(BedTransferred event) {
        if (event.previousFloorId() == event.nextFloorId()) return;
        if (!markers.mark(
                "M04.BED_TRANSFER",
                "M03.ALLOCATION",
                event.nextAllocationId(),
                "REASSIGNED",
                Map.of("at", event.at().toString()))) return;
        locks.lock(List.of(event.residentId()));
        for (var candidate : tasks.openOfResident(event.residentId())) {
            var before = tasks.get(candidate.id(), true);
            if (!OPEN.contains(before.status()) || before.floorAreaId() != event.previousFloorId()) continue;
            var at = before.plannedAt().isAfter(event.at()) ? before.plannedAt() : event.at();
            var shift = commands.staff().shiftContaining(event.nextFloorId(), at);
            Long shiftId = shift.map(s -> s.id()).orElse(before.shiftId());
            Long performer = shift.isEmpty() ? null : assignee(before, shiftId, null);
            var after = tasks.relocate(before, event.nextFloorId(), shiftId, performer);
            commands.record(before, after, "REASSIGNED_AFTER_BED_TRANSFER", null, true);
            if (performer == null)
                notices.shared(after, shift.isEmpty() ? null : shiftId, time.now(), qualified(after));
        }
    }

    /** Phân lại (FR-031, FR-045): Trưởng tầng của tầng; Người phụ trách ca theo nhiệm vụ trong thời gian ca. */
    @RequiresPermission(function = Function.OVERDUE_TASK_HANDLING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reassign(Target target, long id, long recipientId, Reason reason) {
        var before = commands.locked(id);
        if (!OPEN.contains(before.status()))
            throw new BusinessException(HttpStatus.CONFLICT, "CARE_TASK_CLOSED", "Công việc đã đóng");
        if (Objects.equals(before.performerId(), recipientId))
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_PERFORMER", "Người nhận đang thực hiện việc này");
        var staff = commands.staff();
        var shift = commands.shift(before.shiftId());
        var member = staff.members(shift.id()).stream()
                .filter(m -> m.staffId() == recipientId)
                .findFirst();
        if (!shift.published() || member.isEmpty() || !staff.onShift(recipientId, before.plannedAt()))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_PERFORMER", "Người nhận không có ca trùng thời điểm công việc");
        if (!CareTaskValidator.roleMatches(before.performerRole(), member.get().role())
                || !staff.qualified(recipientId, certificates(before), time.today()))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_PERFORMER", "Người nhận không đủ vai trò hoặc chứng chỉ (BR-M09-01)");
        var after = tasks.relocate(before, before.floorAreaId(), before.shiftId(), recipientId);
        commands.record(before, after, "REASSIGNED", reason, false);
    }

    private void share(List<CareTask> candidates, String command) {
        lock(candidates);
        for (var candidate : candidates) {
            var before = tasks.get(candidate.id(), true);
            if (!OPEN.contains(before.status()) || before.performerId() == null) continue;
            var after = tasks.relocate(before, before.floorAreaId(), before.shiftId(), null);
            commands.record(before, after, command, null, true);
            notices.shared(after, after.shiftId(), time.now(), qualified(after));
        }
    }

    private void reassign(List<CareTask> candidates) {
        lock(candidates);
        for (var candidate : candidates) {
            var before = tasks.get(candidate.id(), true);
            if (!OPEN.contains(before.status()) || before.residentId() == null) continue;
            Long performer = assignee(before, before.shiftId(), before.performerId());
            if (Objects.equals(performer, before.performerId())) continue;
            var after = tasks.relocate(before, before.floorAreaId(), before.shiftId(), performer);
            commands.record(before, after, "REASSIGNED_BY_ASSIGNMENT", null, true);
            if (performer == null) notices.shared(after, after.shiftId(), time.now(), qualified(after));
        }
    }

    /** Người được phân công khớp vai trò, đủ chứng chỉ; giữ người hiện tại nếu vẫn hợp lệ, phân công chính trước. */
    private Long assignee(CareTask task, long shiftId, Long current) {
        var certificates = certificates(task);
        var today = time.today();
        var valid = commands.staff().assignments(task.residentId(), shiftId).stream()
                .filter(a -> CareTaskValidator.roleMatches(task.performerRole(), a.role()))
                .filter(a -> commands.staff().qualified(a.staffId(), certificates, today))
                .sorted(Comparator.comparing(a -> !a.primary()))
                .map(a -> a.staffId())
                .toList();
        if (current != null && valid.contains(current)) return current;
        return valid.isEmpty() ? null : valid.getFirst();
    }

    private boolean qualified(CareTask task) {
        return commands.staff().hasQualifiedStaff(task.shiftId(), task.performerRole(), certificates(task));
    }

    private Set<String> certificates(CareTask task) {
        return catalogs.require(CareCatalogKinds.TASK_TYPES, task.data().publicTaskTypeId())
                .attributes()
                .certificates();
    }

    private void lock(List<CareTask> candidates) {
        locks.lock(candidates.stream()
                .map(t -> t.residentId())
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }
}
