package com.eldercare.backend.care.internal.overdue.service;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.api.HandoverTaskApi;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.AlertReadPort;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.domain.CareTaskHandling;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T045: projection đầy đủ005; không tự tạo mục bàn giao. */
@Service
@org.springframework.context.annotation.Fallback
@Transactional(readOnly = true)
public class CareHandoverQueries implements HandoverTaskApi {
    private final CareTaskStore tasks;
    private final CareTaskService commands;
    private final ResidentApi residents;
    private final ObjectProvider<AlertReadPort> alerts;
    private final StatusHistoryApi history;
    private final BusinessTime time;
    private final ReminderMarkers markers;

    public CareHandoverQueries(
            CareTaskStore tasks,
            CareTaskService commands,
            ResidentApi residents,
            ObjectProvider<AlertReadPort> alerts,
            StatusHistoryApi history,
            BusinessTime time,
            ReminderMarkers markers) {
        this.tasks = tasks;
        this.commands = commands;
        this.residents = residents;
        this.alerts = alerts;
        this.history = history;
        this.time = time;
        this.markers = markers;
    }

    public static boolean open(CareTask t) {
        return Set.of(TaskStatus.NOT_YET_DUE, TaskStatus.DUE, TaskStatus.OVERDUE)
                .contains(t.status());
    }

    public List<CareTask> rows(long floor, long shiftId) {
        var shift = commands.shift(shiftId);
        if (shift.floorAreaId() != floor) return List.of();
        return tasks.inShift(shiftId).stream()
                .filter(t -> t.floorAreaId() == floor && open(t))
                .filter(t -> t.residentId() == null || !residents.isFinalStatus(t.residentId()))
                .filter(t -> t.status() != TaskStatus.NOT_YET_DUE
                        || (!t.plannedAt().isBefore(shift.startsAt())
                                && t.plannedAt().isBefore(shift.endsAt())))
                .toList();
    }

    @Override
    public List<HandoverTaskView> unclosed(long floor, long shiftId) {
        var rows = rows(floor, shiftId);
        if (rows.isEmpty()) return List.of();
        var port = alerts.getIfAvailable();
        if (port == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", "Chưa có nguồn cảnh báo cho bàn giao");
        var related = port.byTask(rows.stream().map(CareTask::id).toList());
        return rows.stream()
                .map(t -> new HandoverTaskView(
                        t.id(),
                        Optional.ofNullable(t.residentId()),
                        t.data().publicTaskTypeId(),
                        t.plannedAt(),
                        t.status(),
                        t.importance(),
                        Optional.ofNullable(t.performerId()),
                        reminderCount(t.id()),
                        related.getOrDefault(t.id(), List.of())))
                .toList();
    }

    public int reminderCount(long id) {
        return (int) history.list(CareTaskService.HISTORY, id).stream()
                .filter(h -> Set.of(
                                "REMINDER",
                                "BATCHED_REMINDER",
                                "IMPORTANT_ESCALATION",
                                "NORMAL_ESCALATION",
                                "ALERT_REQUESTED")
                        .contains(h.command()))
                .count();
    }

    @Override
    public Map<Long, TaskStateView> currentStates(Collection<Long> ids) {
        var result = new LinkedHashMap<Long, TaskStateView>();
        for (long id : new TreeSet<>(ids)) {
            var t = tasks.get(id, false);
            var closure = tasks.closure(id);
            var timeline = history.list(CareTaskService.HISTORY, id);
            var actor = closure.at() == null || timeline.isEmpty()
                    ? null
                    : timeline.getLast().actorId();
            result.put(
                    id,
                    new TaskStateView(
                            id,
                            t.status(),
                            Optional.ofNullable(actor),
                            Optional.ofNullable(closure.at()),
                            Optional.ofNullable(closure.rule())));
        }
        return Map.copyOf(result);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordedInHandover(long handoverId, Collection<Long> ids) {
        for (long id : new TreeSet<>(ids)) {
            var task = commands.locked(id);
            if (!open(task)) continue;
            if (!markers.mark("M04.TASK_HANDOVER", "M04.TASK", id, "RECORDED:" + handoverId, null)) continue;
            history.add(
                    CareTaskService.HISTORY,
                    id,
                    new HistoryEntry<>(
                            HistoryKind.POINT,
                            task.status().name(),
                            task.status().name(),
                            null,
                            time.now(),
                            "RECORDED_IN_HANDOVER",
                            null,
                            false,
                            null,
                            "M09.HANDOVER",
                            handoverId,
                            new CareTaskHandling(task.data().content()),
                            task.floorAreaId(),
                            "HANDOVER:" + handoverId + ":" + id));
        }
    }
}
