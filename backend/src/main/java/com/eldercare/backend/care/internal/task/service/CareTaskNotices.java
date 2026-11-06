package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.domain.CareTaskHandling;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.*;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** BR-M04-26: lập muộn có cùng nhắc ban đầu như chuyển Quá hạn; marker cùng transaction. */
@Service
public class CareTaskNotices {
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;
    private final StatusHistoryApi history;

    public CareTaskNotices(ReminderMarkers markers, ApplicationEventPublisher events, StatusHistoryApi history) {
        this.markers = markers;
        this.events = events;
        this.history = history;
    }

    public void overdue(CareTask task, Instant at) {
        initial(List.of(task), at, false);
    }

    public List<RecipientGroup> performer(CareTask task) {
        return task.performerId() == null
                ? List.of(new RecipientGroup.OnShiftStaff(
                        task.floorAreaId(),
                        Set.of(task.performerRole()),
                        task.data().certificates(),
                        new Substitution.Default()))
                : List.of(new RecipientGroup.Staff(task.performerId(), "CARE_TASK_PERFORMER"));
    }

    public void overdueBatch(List<CareTask> tasks, Instant at) {
        var groups = new LinkedHashMap<List<Object>, List<CareTask>>();
        for (var task : tasks)
            groups.computeIfAbsent(List.of(task.floorAreaId(), performer(task)), k -> new ArrayList<>())
                    .add(task);
        for (var group : groups.values()) initial(group, at, true);
    }

    public void shared(CareTask task, Long shiftId, Instant at, boolean qualifiedStaff) {
        var recipients = List.<RecipientGroup>of(
                new RecipientGroup.FloorManager(task.floorAreaId(), new Substitution.Default()));
        events.publishEvent(new TaskBecameShared(
                task.id(), task.floorAreaId(), context(task, at, "SHARED:" + shiftId, recipients)));
        if (shiftId != null && !qualifiedStaff)
            events.publishEvent(new SharedTaskWithoutQualifiedStaff(
                    task.id(),
                    shiftId,
                    task.floorAreaId(),
                    context(task, at, "NO_QUALIFIED_STAFF:" + shiftId, recipients)));
    }

    private void initial(List<CareTask> tasks, Instant at, boolean batch) {
        var fresh = new ArrayList<CareTask>();
        for (var task : tasks) {
            if (!markers.mark("M04.TASK_OVERDUE", "M04.TASK", task.id(), "INITIAL", Map.of("at", at.toString())))
                continue;
            fresh.add(task);
            handling(task, at, batch ? "BATCHED_REMINDER" : "REMINDER");
            escalateInitial(task, at);
        }
        if (fresh.isEmpty()) return;
        if (batch)
            events.publishEvent(new BatchedOverdueTaskReminder(
                    fresh.getFirst().floorAreaId(),
                    fresh.size(),
                    fresh.stream()
                            .map(CareTask::plannedAt)
                            .min(Instant::compareTo)
                            .orElseThrow(),
                    fresh.stream()
                            .map(CareTask::plannedAt)
                            .max(Instant::compareTo)
                            .orElseThrow(),
                    at,
                    context(fresh.getFirst(), at, "BATCHED_REMINDER", performer(fresh.getFirst()))));
        else
            events.publishEvent(new OverdueTaskReminder(
                    fresh.getFirst().id(), context(fresh.getFirst(), at, "REMINDER", performer(fresh.getFirst()))));
    }

    private void escalateInitial(CareTask task, Instant at) {
        var leads = List.<RecipientGroup>of(
                new RecipientGroup.FloorManager(task.floorAreaId(), new Substitution.Default()),
                new RecipientGroup.ShiftLead(task.floorAreaId(), new Substitution.Default()));
        if (task.importance() == Importance.IMPORTANT) {
            events.publishEvent(new OverdueTaskNeedsHandling(
                    task.id(),
                    task.importance(),
                    context(
                            task,
                            at,
                            "IMPORTANT",
                            List.of(new RecipientGroup.FloorManager(task.floorAreaId(), new Substitution.Default())))));
            handling(task, at, "IMPORTANT_ESCALATION");
        }
        if (task.importance() == Importance.MANDATORY) {
            events.publishEvent(new AlertRequested(
                    "OVERDUE_CARE_TASK",
                    NotificationLevel.MEDIUM,
                    task.residentId(),
                    "M04.TASK:" + task.id(),
                    task.basisType(),
                    task.basisId() == null ? task.id() : task.basisId(),
                    context(task, at, "MANDATORY", leads),
                    Optional.of(new CareAlertSource(
                            task.locationType(),
                            task.locationId(),
                            task.floorAreaId(),
                            task.data().publicTaskTypeId(),
                            task.data().taskTypeName(),
                            task.plannedAt(),
                            task.windowFrom(),
                            task.windowTo(),
                            task.importance()))));
            handling(task, at, "ALERT_REQUESTED");
        }
    }

    public void escalateNormal(CareTask task, Instant at) {
        if (!markers.mark("M04.TASK_OVERDUE", "M04.TASK", task.id(), "NORMAL_ESCALATION", Map.of("at", at.toString())))
            return;
        events.publishEvent(new OverdueTaskNeedsHandling(
                task.id(),
                task.importance(),
                context(
                        task,
                        at,
                        "NORMAL_ESCALATION",
                        List.of(new RecipientGroup.ShiftLead(task.floorAreaId(), new Substitution.Default())))));
        handling(task, at, "NORMAL_ESCALATION");
    }

    private void handling(CareTask task, Instant at, String command) {
        history.add(
                CareTaskService.HISTORY,
                task.id(),
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        task.status().name(),
                        task.status().name(),
                        null,
                        at,
                        command,
                        null,
                        true,
                        null,
                        task.basisType().name(),
                        task.basisId(),
                        new CareTaskHandling(task.data().content()),
                        task.floorAreaId(),
                        null));
    }

    private CareEventContext context(CareTask task, Instant at, String milestone, List<RecipientGroup> recipients) {
        return new CareEventContext("M04.TASK", task.id(), milestone, at, Optional.of(task.floorAreaId()), recipients);
    }
}
