package com.eldercare.backend.reporting.internal.activity.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.activity.service.ActivityRecords.SessionRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code ACT.SESSIONS} ({@code HD.BUOI}; FR-020, FR-035): số buổi theo trạng thái buổi do 014 xác định.
 */
@Component
@Transactional(readOnly = true)
public class SessionsMetric implements MetricSource<SessionRecord> {

    public static final String CODE = "ACT.SESSIONS";

    private static final Metric<SessionRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.ACTIVITIES,
            "Buổi hoạt động",
            "Số buổi bắt đầu trong khoảng theo trạng thái buổi; số đăng ký, Có mặt, Vắng theo lý do, Không ghi nhận ở dòng chi tiết",
            "014 ActivityStatisticsApi.sessions",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().status() == null ? "UNKNOWN" : r.view().status(),
            ActivityRecords::sessionRow);

    private final ActivityRecords records;

    public SessionsMetric(ActivityRecords records) {
        this.records = records;
    }

    @Override
    public Metric<SessionRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<SessionRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.sessions(rough, from, to);
    }
}
