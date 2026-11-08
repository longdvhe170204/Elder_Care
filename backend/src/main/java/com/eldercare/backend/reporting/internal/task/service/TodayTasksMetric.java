package com.eldercare.backend.reporting.internal.task.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.task.service.TaskRecords.TaskRecord;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code TASK.TODAY} ({@code CV.HOM_NAY}; FR-020): số công việc có thời điểm dự kiến trong khoảng, chia theo
 * trạng thái 8.3 do 005 xác định.
 */
@Component
@Transactional(readOnly = true)
public class TodayTasksMetric implements MetricSource<TaskRecord> {

    public static final String CODE = "TASK.TODAY";

    private static final Metric<TaskRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.CARE_TASKS,
            "Công việc hôm nay",
            "Số công việc có thời điểm dự kiến trong khoảng (dashboard: hôm nay), chia theo trạng thái 8.3; mức quan trọng ở dòng chi tiết",
            "005 TaskStatisticsApi.inRange",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().status().name(),
            TaskRecords::row);

    private final TaskRecords records;

    public TodayTasksMetric(TaskRecords records) {
        this.records = records;
    }

    @Override
    public Metric<TaskRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<TaskRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to);
    }
}
