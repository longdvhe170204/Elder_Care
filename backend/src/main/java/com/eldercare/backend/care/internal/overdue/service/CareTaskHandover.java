package com.eldercare.backend.care.internal.overdue.service;

import com.eldercare.backend.care.api.ShiftTaskApi;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.catalog.domain.CareCatalogKinds;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskNotices;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.care.internal.task.service.CareTaskValidator;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T045: callback đồng bộ của lệnh008, không tự tạo ca hoặc mục bàn giao. */
@Service
@org.springframework.context.annotation.Fallback
public class CareTaskHandover implements ShiftTaskApi {
    private final CareTaskStore tasks;
    private final CareTaskService commands;
    private final ReminderMarkers markers;
    private final CareTaskNotices notices;
    private final CareLockStore locks;
    private final CareCatalogStore catalogs;

    public CareTaskHandover(
            CareTaskStore tasks,
            CareTaskService commands,
            ReminderMarkers markers,
            CareTaskNotices notices,
            CareLockStore locks,
            CareCatalogStore catalogs) {
        this.tasks = tasks;
        this.commands = commands;
        this.markers = markers;
        this.notices = notices;
        this.locks = locks;
        this.catalogs = catalogs;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public HandoverTransferResult onHandoverConfirmed(
            long handoverId, long previousShiftId, Long nextShiftId, Set<Long> taskIds, Instant at) {
        var previous = commands.shift(previousShiftId);
        if (nextShiftId != null && commands.shift(nextShiftId).floorAreaId() != previous.floorAreaId())
            throw new BusinessException(HttpStatus.CONFLICT, "OUTSIDE_SHIFT", "Ca sau phải cùng tầng/khu vực");
        var closed = new ArrayList<Long>();
        var transferred = new ArrayList<Long>();
        var candidates = tasks.inShift(previousShiftId);
        locks.lock(candidates.stream()
                .map(t -> t.residentId())
                .filter(Objects::nonNull)
                .toList());
        for (var candidate : candidates.stream()
                .sorted(Comparator.comparingLong(t -> t.id()))
                .toList()) {
            var before = commands.locked(candidate.id());
            if (before.shiftId() != previousShiftId || !CareHandoverQueries.open(before)) continue;
            if (!markers.mark(
                    "M04.TASK_HANDOVER",
                    "M04.TASK",
                    before.id(),
                    "CONFIRMED:" + handoverId,
                    Map.of("at", at.toString()))) continue;
            boolean claimedWhileWaiting =
                    "AWAITING_HANDOVER_CONFIRMATION".equals(before.carryover()) && before.performerId() != null;
            if (!claimedWhileWaiting
                    && before.importance() == Importance.NORMAL
                    && before.status() == TaskStatus.OVERDUE
                    && taskIds.contains(before.id())) {
                var reason = new Reason("hệ thống đóng sau bàn giao");
                var after = tasks.transition(
                        before, TaskStatus.NOT_PERFORMED, at, reason.value(), "SYSTEM_CLOSED_AFTER_HANDOVER");
                commands.record(before, after, "SYSTEM_CLOSED_AFTER_HANDOVER", reason, true, handoverId);
                closed.add(before.id());
            } else if (!"FROM_PREVIOUS_SHIFT".equals(before.carryover()) || nextShiftId != null) {
                var certificates = catalogs.require(
                                CareCatalogKinds.TASK_TYPES, before.data().publicTaskTypeId())
                        .attributes()
                        .certificates();
                Long performer = before.performerId();
                if (!claimedWhileWaiting)
                    performer = nextShiftId == null || before.residentId() == null
                            ? null
                            : commands.staff().assignments(before.residentId(), nextShiftId).stream()
                                    .filter(a -> CareTaskValidator.roleMatches(before.performerRole(), a.role()))
                                    .filter(a -> commands.staff()
                                            .qualified(
                                                    a.staffId(),
                                                    certificates,
                                                    at.atZone(BusinessTime.ZONE).toLocalDate()))
                                    .map(a -> a.staffId())
                                    .findFirst()
                                    .orElse(null);
                var after = tasks.carry(before, nextShiftId, performer, "FROM_PREVIOUS_SHIFT");
                commands.record(before, after, "TRANSFERRED_AFTER_HANDOVER", null, true, handoverId);
                if (performer == null)
                    notices.shared(
                            after,
                            nextShiftId,
                            at,
                            nextShiftId != null
                                    && commands.staff()
                                            .hasQualifiedStaff(nextShiftId, before.performerRole(), certificates));
                transferred.add(before.id());
            }
        }
        return new HandoverTransferResult(closed, transferred);
    }

    // Các callback vắng/đổi phân công còn thiếu ở T038; không bỏ qua tác động bằng no-op.
    private BusinessException pending() {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CARE_SOURCE_UNAVAILABLE",
                "Callback vắng/đổi phân công chưa được triển khai trong Phase 7");
    }

    @Override
    public void onAbsence(long staffId, long shiftId) {
        throw pending();
    }

    @Override
    public void onApprovedLeave(long staffId, Set<Long> ids) {
        throw pending();
    }

    @Override
    public void onResignation(long staffId, Instant at) {
        throw pending();
    }

    @Override
    public void onAssignmentChanged(long residentId, long shiftId) {
        throw pending();
    }

    @Override
    public void onAssignmentEnded(long assignmentId, long shiftId) {
        throw pending();
    }
}
