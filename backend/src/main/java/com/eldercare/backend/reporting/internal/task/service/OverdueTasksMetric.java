package com.eldercare.backend.reporting.internal.task.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.dto.TaskStatus;
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
 * Chỉ tiêu {@code TASK.OVERDUE} ({@code CV.QUA_HAN}; FR-020, FR-030): báo cáo đếm công việc đã từng ở Quá hạn trong
 * khoảng theo mốc 005 ghi (bản ghi muộn sau đó Hoàn thành vẫn tính); dashboard đếm công việc đang Quá hạn lúc xem. Chia theo
 * mức quan trọng. Báo cáo chỉ xét công việc có thời điểm dự kiến trong khoảng (plan 016, ghi chú 2026-11-08).
 */
@Component
@Transactional(readOnly = true)
public class OverdueTasksMetric implements MetricSource<TaskRecord> {

    public static final String CODE = "TASK.OVERDUE";

    private static final Metric<TaskRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.CARE_TASKS,
            "Công việc quá hạn",
            "Báo cáo: công việc có thời điểm dự kiến trong khoảng đã từng vào Quá hạn trong khoảng, kể cả sau đó Hoàn thành; dashboard: công việc đang Quá hạn lúc xem (mọi ngày dự kiến); chia theo mức quan trọng",
            "005 TaskStatisticsApi.inRange (overdueAt, status)",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().importance().name(),
            TaskRecords::row);

    private final TaskRecords records;

    public OverdueTasksMetric(TaskRecords records) {
        this.records = records;
    }

    @Override
    public Metric<TaskRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<TaskRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(r -> r.overdueWithin(from, to));
    }

    /** Dashboard (FR-020): đang ở Quá hạn lúc xem, mọi ngày dự kiến. */
    @Override
    public Stream<TaskRecord> current(ScopeFilter rough, Instant from, Instant to) {
        return records.in(rough, Instant.EPOCH, to).filter(r -> r.view().status() == TaskStatus.OVERDUE);
    }
}
