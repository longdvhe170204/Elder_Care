package com.eldercare.backend.reporting.internal.task.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.task.service.TaskRecords.TaskRecord;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code TASK.MANDATORY_ON_TIME_RATE} ({@code CV.TY_LE_DUNG_HAN_BAT_BUOC}; FR-031): số Hoàn thành ÷ (Hoàn
 * thành + Hoàn thành trễ + Không thực hiện) của công việc Bắt buộc; Không thực hiện do Hệ thống đóng không vào mẫu số (số
 * này đọc ở {@code TASK.BY_STATUS}); công việc từng Quá hạn nhưng trạng thái cuối Hoàn thành là đúng hạn.
 */
@Component
@Transactional(readOnly = true)
public class MandatoryOnTimeRateMetric implements MetricSource<TaskRecord> {

    public static final String CODE = "TASK.MANDATORY_ON_TIME_RATE";

    private static final Metric<TaskRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.CARE_TASKS,
            "Tỷ lệ hoàn thành đúng hạn công việc Bắt buộc (%)",
            "Số Hoàn thành ÷ (Hoàn thành + Hoàn thành trễ + Không thực hiện), công việc Bắt buộc có thời điểm dự kiến trong khoảng; không gồm Hủy, việc chưa ở trạng thái cuối, Không thực hiện do Hệ thống đóng",
            "005 TaskStatisticsApi.inRange",
            new Metric.Computation.Ratio<>(
                    MandatoryOnTimeRateMetric::inDenominator, r -> r.view().status() == TaskStatus.COMPLETED),
            null,
            TaskRecords::row);

    private final TaskRecords records;

    public MandatoryOnTimeRateMetric(TaskRecords records) {
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

    private static final Set<TaskStatus> FINAL =
            Set.of(TaskStatus.COMPLETED, TaskStatus.COMPLETED_LATE, TaskStatus.NOT_PERFORMED);

    static boolean inDenominator(TaskRecord r) {
        return r.view().importance() == Importance.MANDATORY
                && FINAL.contains(r.view().status())
                && !r.closedBySystem();
    }
}
