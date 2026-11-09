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
 * Chỉ tiêu {@code ACT.PARTICIPATION_RATE} ({@code HD.TY_LE_THAM_GIA}; FR-035, Q-169): tỷ lệ tham gia theo hoạt động; tầng ở
 * bộ lọc chung.
 */
@Component
@Transactional(readOnly = true)
public class ParticipationRateMetric implements MetricSource<AttendanceRecord> {

    public static final String CODE = "ACT.PARTICIPATION_RATE";

    private static final Metric<AttendanceRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.ACTIVITIES,
            "Tỷ lệ tham gia hoạt động (%)",
            "Số lượt Có mặt ÷ (Có mặt + Vắng lý do từ chối, sức khỏe, khác), chia theo hoạt động; Vắng – đang vắng mặt, Vắng – không đến, Không ghi nhận, đăng ký Đã hủy không vào mẫu số",
            "014 ActivityStatisticsApi.sessions",
            new Metric.Computation.Ratio<>(r -> true, AttendanceRecord::present),
            r -> Long.toString(r.session().activityId()),
            ActivityRecords::attendanceRow);

    private final ActivityRecords records;

    public ParticipationRateMetric(ActivityRecords records) {
        this.records = records;
    }

    @Override
    public Metric<AttendanceRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<AttendanceRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.attendance(rough, from, to);
    }
}
