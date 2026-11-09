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
 * Chỉ tiêu {@code TASK.LATE_RECORDINGS} ({@code CV.GHI_NHAN_MUON}; FR-032, BR-M04-12): kết quả mang nhãn "ghi nhận
 * muộn" do 005 gắn, chia theo nhân viên. Điểm danh buổi hoạt động mang nhãn này tính riêng: nguồn 014 chưa trả nhãn (plan 016,
 * ghi chú 2026-11-08).
 */
@Component
@Transactional(readOnly = true)
public class LateRecordingsMetric implements MetricSource<TaskRecord> {

    public static final String CODE = "TASK.LATE_RECORDINGS";

    private static final Metric<TaskRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.CARE_TASKS,
            "Ghi nhận muộn",
            "Số công việc có thời điểm dự kiến trong khoảng mà kết quả mang nhãn ghi nhận muộn (CFG-M04-06), chia theo nhân viên thực hiện; ca, tầng ở bộ lọc chung",
            "005 TaskStatisticsApi.inRange (recordedLate)",
            new Metric.Computation.Count<>(r -> true),
            TaskRecord::staffKey,
            TaskRecords::row);

    private final TaskRecords records;

    public LateRecordingsMetric(TaskRecords records) {
        this.records = records;
    }

    @Override
    public Metric<TaskRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<TaskRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(r -> r.view().recordedLate());
    }
}
