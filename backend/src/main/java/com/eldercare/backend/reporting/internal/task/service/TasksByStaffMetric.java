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
 * Chỉ tiêu {@code TASK.BY_STAFF} ({@code CV.THEO_NHAN_VIEN}; FR-030, FR-037): công việc theo nhân viên thực hiện; công
 * việc chung của tầng (không gắn người cao tuổi) ở dòng {@code COMMON}. Trạng thái ở dòng chi tiết.
 */
@Component
@Transactional(readOnly = true)
public class TasksByStaffMetric implements MetricSource<TaskRecord> {

    public static final String CODE = "TASK.BY_STAFF";

    private static final Metric<TaskRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.PER_STAFF,
            "Công việc theo nhân viên",
            "Số công việc có thời điểm dự kiến trong khoảng theo nhân viên thực hiện; việc chung của tầng một dòng riêng, việc chưa có người thực hiện một dòng",
            "005 TaskStatisticsApi.inRange",
            new Metric.Computation.Count<>(r -> true),
            TaskRecord::staffKey,
            TaskRecords::row);

    private final TaskRecords records;

    public TasksByStaffMetric(TaskRecords records) {
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
