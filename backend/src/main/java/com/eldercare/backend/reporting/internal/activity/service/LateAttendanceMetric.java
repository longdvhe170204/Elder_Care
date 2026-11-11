package com.eldercare.backend.reporting.internal.activity.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.activity.service.ActivityRecords.AttendanceRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code ACT.LATE_ATTENDANCE} (FR-032, BR-M04-12; 014 FR-030): số điểm danh buổi hoạt động mang nhãn "ghi nhận
 * muộn", tính riêng với kết quả công việc ghi nhận muộn ({@code TASK.LATE_RECORDINGS}), chia theo hoạt động; tầng ở
 * bộ lọc chung.
 */
@Component
@Transactional(readOnly = true)
public class LateAttendanceMetric implements MetricSource<AttendanceRecord> {

    public static final String CODE = "ACT.LATE_ATTENDANCE";

    private static final Metric<AttendanceRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.ACTIVITIES,
            "Điểm danh hoạt động ghi nhận muộn",
            "Số điểm danh buổi hoạt động mang nhãn ghi nhận muộn (ghi sau giờ kết thúc buổi quá CFG-M04-06), buổi bắt đầu trong khoảng, chia theo hoạt động",
            "014 ActivityStatisticsApi.sessions",
            new Metric.Computation.Count<>(r -> true),
            r -> Long.toString(r.session().activityId()),
            ActivityRecords::attendanceRow);

    private final ActivityRecords records;

    public LateAttendanceMetric(ActivityRecords records) {
        this.records = records;
    }

    @Override
    public Metric<AttendanceRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<AttendanceRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.lateAttendance(rough, from, to);
    }
}
