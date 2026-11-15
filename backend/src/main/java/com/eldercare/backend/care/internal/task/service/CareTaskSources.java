package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.care.api.TaskSourceApi;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.TaskSourceChanged;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.*;
import org.springframework.context.*;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T025/FR-022: callback luôn trong transaction lệnh nguồn, không tự tạo công việc. */
@Service
@Fallback
@Transactional(propagation = Propagation.MANDATORY)
public class CareTaskSources implements TaskSourceApi {
    private final CareTaskStore tasks;
    private final CareTaskService commands;
    private final CareLockStore locks;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public CareTaskSources(
            CareTaskStore tasks,
            CareTaskService commands,
            CareLockStore locks,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.tasks = tasks;
        this.commands = commands;
        this.locks = locks;
        this.events = events;
        this.time = time;
    }

    @Override
    public void sourceCancelled(SourceType type, long sourceId, Reason reason) {
        cancel(tasks.forSource(type, sourceId), reason);
    }

    @Override
    public void sourceCancelled(SourceType type, long sourceId, long residentId, Reason reason) {
        cancel(
                tasks.forSource(type, sourceId).stream()
                        .filter(t -> Objects.equals(t.residentId(), residentId))
                        .toList(),
                reason);
    }

    private void cancel(List<com.eldercare.backend.care.internal.task.domain.CareTask> candidates, Reason reason) {
        Objects.requireNonNull(reason);
        locks.lock(candidates.stream()
                .map(t -> t.residentId())
                .filter(Objects::nonNull)
                .toList());
        for (var candidate : candidates) {
            var before = tasks.get(candidate.id(), true);
            if (before.status() != TaskStatus.NOT_YET_DUE) continue;
            // Nếu lịch đang chạy bù, không hủy việc đã thực tế tới hạn.
            var current = com.eldercare.backend.care.internal.task.domain.CareTaskTransitions.initialStatus(
                    before.windowFrom(), before.windowTo(), time.now());
            if (current != TaskStatus.NOT_YET_DUE) {
                var after = tasks.transition(before, current, time.now(), null, null);
                commands.record(before, after, "TIME_CHANGED", null, true);
                continue;
            }
            var after = tasks.transition(before, TaskStatus.CANCELLED, time.now(), reason.value(), "SOURCE_CANCELLED");
            commands.record(before, after, "SOURCE_CANCELLED", reason, true);
        }
    }

    @Override
    public void sourceChanged(SourceType type, long sourceId) {
        var seen = new HashSet<Long>();
        var now = time.now();
        for (var task : tasks.forSource(type, sourceId)) {
            if (task.residentId() == null || !seen.add(task.residentId())) continue;
            var context = new CareEventContext(
                    "M04.SOURCE",
                    sourceId,
                    "CHANGE:" + task.residentId() + ":" + now,
                    now,
                    Optional.of(task.floorAreaId()),
                    List.of(
                            new RecipientGroup.ShiftLead(task.floorAreaId(), new Substitution.Default()),
                            new RecipientGroup.FloorManager(task.floorAreaId(), new Substitution.Default())));
            events.publishEvent(new TaskSourceChanged(type, sourceId, task.residentId(), context));
        }
    }
}
