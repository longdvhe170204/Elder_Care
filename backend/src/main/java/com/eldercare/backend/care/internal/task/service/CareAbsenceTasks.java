package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.care.api.AbsenceTaskApi;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.dto.CareTimeRange;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.events.TaskPlanningNeeded;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T057, research C-08: hủy vì vắng mặt và báo cần lập công việc; không tự tạo hay mở lại công việc (Q-306). */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CareAbsenceTasks implements AbsenceTaskApi {
    public static final String ABSENCE_RULE = "ABSENCE_CANCELLED";
    private static final Reason ABSENCE = new Reason("vắng mặt");

    private final CareTaskStore tasks;
    private final CareTaskService commands;
    private final CareLockStore locks;
    private final BedAllocationApi beds;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public CareAbsenceTasks(
            CareTaskStore tasks,
            CareTaskService commands,
            CareLockStore locks,
            BedAllocationApi beds,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.tasks = tasks;
        this.commands = commands;
        this.locks = locks;
        this.beds = beds;
        this.events = events;
        this.time = time;
    }

    /** FR-020, BR-M04-04: Chưa đến hạn có thời điểm dự kiến từ lúc rời; Đến hạn, Quá hạn giữ nguyên (FR-027). */
    public void departed(long residentId, Instant at) {
        locks.lock(List.of(residentId));
        cancel(tasks.notYetDueFrom(residentId, at));
    }

    /** FR-020, FR-017: chỉ báo Người phụ trách ca của tầng. */
    public void planningNeeded(long residentId, String reason, Instant at, String milestone) {
        var floor = beds.openAt(residentId, at).map(BedAllocationView::floorAreaId);
        if (floor.isEmpty()) return;
        var context = new CareEventContext(
                "M04.RESIDENT",
                residentId,
                milestone,
                at,
                floor,
                List.of(new RecipientGroup.ShiftLead(floor.get(), new Substitution.Default())));
        events.publishEvent(new TaskPlanningNeeded(residentId, reason, at, context));
    }

    /** FR-020a: đính chính hoặc hủy ghi nhận khoảng đi (khoảng rỗng là không còn khoảng đi). */
    @Override
    public void recalculate(long residentId, Optional<CareTimeRange> previous, Optional<CareTimeRange> current) {
        locks.lock(List.of(residentId));
        var now = time.now();
        current.ifPresent(range -> cancel(tasks.notYetDueFrom(residentId, range.from()).stream()
                .filter(t -> inside(range, t.plannedAt()))
                .toList()));
        var reopened = tasks.cancelledForAbsenceAfter(residentId, now).stream()
                .filter(t -> previous.map(r -> inside(r, t.plannedAt())).orElse(true))
                .filter(t -> current.map(r -> !inside(r, t.plannedAt())).orElse(true))
                .toList();
        if (reopened.isEmpty()) return;
        var first = reopened.getFirst().plannedAt();
        planningNeeded(residentId, "ABSENCE_RECALCULATED", first, "ABSENCE_RECALCULATED:" + now + ":" + first);
    }

    private void cancel(List<CareTask> candidates) {
        for (var candidate : candidates) {
            var before = tasks.get(candidate.id(), true);
            if (before.status() != TaskStatus.NOT_YET_DUE) continue;
            var after = tasks.transition(before, TaskStatus.CANCELLED, time.now(), ABSENCE.value(), ABSENCE_RULE);
            commands.record(before, after, "CANCELLED_ON_ABSENCE", ABSENCE, true);
        }
    }

    private static boolean inside(CareTimeRange range, Instant at) {
        return !at.isBefore(range.from()) && (range.to() == null || at.isBefore(range.to()));
    }
}
