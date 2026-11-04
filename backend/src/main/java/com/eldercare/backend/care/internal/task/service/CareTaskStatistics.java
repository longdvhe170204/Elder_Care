package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.attendance.persistence.AttendanceStore;
import com.eldercare.backend.care.internal.attendance.service.AttendanceQueries;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.care.internal.rules.service.CurrentCareResults;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T059 (module-interfaces mục 1.9): số liệu cho 016 và 007, 014; chỉ đọc, không tính chỉ tiêu ở 005. */
@Service
@Transactional(readOnly = true)
public class CareTaskStatistics implements TaskStatisticsApi {
    static final String PERMISSION_LOST = "SYNCED_AFTER_PERMISSION_LOST";
    static final String NONE = "NONE";

    private final CareTaskStore tasks;
    private final CareResultStore results;
    private final CurrentCareResults current;
    private final StatusHistoryApi history;
    private final AttendanceStore attendance;

    public CareTaskStatistics(
            CareTaskStore tasks,
            CareResultStore results,
            CurrentCareResults current,
            StatusHistoryApi history,
            AttendanceStore attendance) {
        this.tasks = tasks;
        this.results = results;
        this.current = current;
        this.history = history;
        this.attendance = attendance;
    }

    @Override
    public Stream<TaskReportView> inRange(ScopeFilter scope, Instant from, Instant to) {
        return tasks.inRange(scope, from, to).stream().map(this::view);
    }

    @Override
    public Stream<AttendanceView> attendanceInRange(ScopeFilter scope, LocalDate from, LocalDate to) {
        return from.datesUntil(to.plusDays(1))
                .flatMap(date -> attendance.onDate(date).stream())
                .filter(row -> scope.facilityWide()
                        || scope.floorAreaIds().contains(row.content().floorAreaId())
                        || (row.residentId() != null && scope.residentIds().contains(row.residentId())))
                .map(AttendanceQueries::view);
    }

    /** Giá trị hiện hành sau đính chính; kết quả đã Hủy ghi nhận không trả (giống quy tắc mục F). */
    @Override
    public Stream<CareResultView> resultsInRange(long residentId, Set<ResultType> types, Instant from, Instant to) {
        return current.read(residentId, from, to).stream()
                .filter(v -> types.contains(ResultType.valueOf(v.original().resultType())))
                .sorted(Comparator.comparing(
                                (CurrentCareResults.Value v) -> v.original().performedAt())
                        .thenComparingLong(v -> v.original().id()))
                .map(v -> new CareResultView(
                        v.original().id(),
                        Optional.of(v.task().id()),
                        residentId,
                        ResultType.valueOf(v.original().resultType()),
                        v.original().performedAt(),
                        Optional.ofNullable(v.original().deviceAt()),
                        v.original().recordedBy(),
                        values(v.values()),
                        Optional.ofNullable(v.values().get("note")).map(Object::toString)));
    }

    /** Người ghi kết quả gốc (tài khoản, cùng nghĩa với {@code CareResultView.recordedById}), kể cả công việc chung. */
    @Override
    public Set<Long> resultRecorders(long residentId, Instant from, Instant to) {
        var ids = new TreeSet<Long>();
        for (var row : results.history(residentId, from, to)) ids.add(row.recordedBy());
        return Collections.unmodifiableSet(ids);
    }

    private TaskReportView view(CareTask task) {
        var changes = history.list(CareTaskService.HISTORY, task.id()).stream()
                .filter(h -> h.value() != null)
                .map(h -> new TaskStateChange(TaskStatus.valueOf(h.value()), h.startsAt()))
                .toList();
        var result = results.find(task.id());
        return new TaskReportView(
                task.id(),
                Optional.ofNullable(task.residentId()),
                task.taskTypeId(),
                task.floorAreaId(),
                task.shiftId(),
                task.status(),
                changes,
                task.importance(),
                Optional.ofNullable(task.performerId()),
                Optional.ofNullable(results.closedBy(task.id())),
                Optional.ofNullable(tasks.closure(task.id()).rule()),
                task.plannedAt(),
                result.map(r -> r.performedAt()),
                result.map(r -> r.deviceAt()),
                result.map(r -> r.lateRecording()).orElse(false),
                result.filter(r -> r.permissionLost()).isPresent() ? PERMISSION_LOST : NONE,
                changes.stream()
                        .filter(c -> c.status() == TaskStatus.OVERDUE)
                        .map(TaskStateChange::at)
                        .toList());
    }

    private static Map<String, Object> values(Map<?, ?> source) {
        var out = new LinkedHashMap<String, Object>();
        source.forEach((k, v) -> out.put(String.valueOf(k), v));
        return Collections.unmodifiableMap(out);
    }
}
