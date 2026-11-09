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
 * Chỉ tiêu {@code TASK.BY_STATUS} ({@code CV.THEO_TRANG_THAI}; FR-030): công việc theo trạng thái 8.3; công việc đóng bởi
 * "Hệ thống" tách khỏi việc nhân viên đóng, theo quy tắc đóng 005 FR-028 ({@code <trạng thái>:SYSTEM:<quy tắc>}). Ca, tầng,
 * loại công việc, mức quan trọng ở bộ lọc chung và dòng chi tiết.
 */
@Component
@Transactional(readOnly = true)
public class TasksByStatusMetric implements MetricSource<TaskRecord> {

    public static final String CODE = "TASK.BY_STATUS";

    private static final Metric<TaskRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.CARE_TASKS,
            "Công việc theo trạng thái",
            "Số công việc có thời điểm dự kiến trong khoảng theo trạng thái 8.3; việc Không thực hiện, Hủy do Hệ thống đóng tách theo quy tắc đóng của 005",
            "005 TaskStatisticsApi.inRange",
            new Metric.Computation.Count<>(r -> true),
            TaskRecord::statusKey,
            TaskRecords::row);

    private final TaskRecords records;

    public TasksByStatusMetric(TaskRecords records) {
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
