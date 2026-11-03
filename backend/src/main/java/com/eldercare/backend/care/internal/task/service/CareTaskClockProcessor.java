package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.catalog.domain.CareCatalogKinds;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T044: khóa người rồi khóa công việc, kiểm lại trạng thái trong cùng transaction. */
@Service
public class CareTaskClockProcessor {
    private final CareTaskStore tasks;
    private final CareTaskService commands;
    private final CareTaskNotices notices;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final CareLockStore locks;
    private final CareCatalogStore catalogs;

    public CareTaskClockProcessor(
            CareTaskStore tasks,
            CareTaskService commands,
            CareTaskNotices notices,
            ResidentApi residents,
            ParameterApi parameters,
            CareLockStore locks,
            CareCatalogStore catalogs) {
        this.tasks = tasks;
        this.commands = commands;
        this.notices = notices;
        this.residents = residents;
        this.parameters = parameters;
        this.locks = locks;
        this.catalogs = catalogs;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int advance(long id, Instant now) {
        return advance(id, now, null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int advanceBatch(List<Long> ids, Instant now) {
        var rows = ids.stream().map(id -> tasks.get(id, false)).toList();
        locks.lock(
                rows.stream().map(CareTask::residentId).filter(Objects::nonNull).toList());
        var reminders = new ArrayList<CareTask>();
        int count = 0;
        for (long id : ids.stream().sorted().toList()) count += advance(id, now, reminders);
        notices.overdueBatch(reminders, now);
        return count;
    }

    private int advance(long id, Instant now, List<CareTask> reminders) {
        var task = commands.locked(id);
        if (task.residentId() != null && residents.isFinalStatus(task.residentId())) return 0;
        if (!java.util.Set.of(TaskStatus.NOT_YET_DUE, TaskStatus.DUE, TaskStatus.OVERDUE)
                .contains(task.status())) return 0;
        int count = 0;
        if (task.status() != TaskStatus.OVERDUE && !now.isBefore(task.windowFrom())) {
            var next = !now.isBefore(task.windowTo()) ? TaskStatus.OVERDUE : TaskStatus.DUE;
            if (next != task.status()) {
                var after = tasks.transition(task, next, now, null, null);
                commands.record(task, after, "TIME_CHANGED", null, true);
                task = after;
                if (next == TaskStatus.OVERDUE) {
                    if (reminders == null) notices.overdue(task, now);
                    else reminders.add(task);
                }
                count++;
            }
        }
        if (task.status() == TaskStatus.OVERDUE && task.importance() == Importance.NORMAL) {
            var since = tasks.overdueAt(id);
            if (since != null
                    && !now.isBefore(
                            since.plus(parameters.read(ParameterCode.CFG_M04_12).asDuration())))
                notices.escalateNormal(task, now);
        }
        var old = commands.shift(task.shiftId());
        if (!"AWAITING_HANDOVER_CONFIRMATION".equals(task.carryover())
                && !now.isBefore(old.endsAt())
                && !commands.staff().handoverConfirmed(old.id())) {
            var next = commands.staff().shiftContaining(task.floorAreaId(), now);
            if (next.isPresent()
                    && next.get().id() != old.id()
                    && (task.performerId() == null || !commands.staff().onShift(task.performerId(), now))) {
                var after = tasks.carry(task, null, null, "AWAITING_HANDOVER_CONFIRMATION");
                commands.record(task, after, "AWAITING_HANDOVER_CONFIRMATION", null, true);
                notices.shared(
                        after,
                        next.get().id(),
                        now,
                        commands.staff()
                                .hasQualifiedStaff(
                                        next.get().id(),
                                        task.performerRole(),
                                        catalogs.require(
                                                        CareCatalogKinds.TASK_TYPES,
                                                        task.data().publicTaskTypeId())
                                                .attributes()
                                                .certificates()));
                count++;
            }
        }
        return count;
    }
}
