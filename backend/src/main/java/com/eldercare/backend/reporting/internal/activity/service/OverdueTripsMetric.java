package com.eldercare.backend.reporting.internal.activity.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.activity.service.ActivityRecords.TripRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code ACT.OVERDUE_TRIPS} ({@code HD.CHUYEN_QUA_GIO}; FR-020, FR-035, BR-M04-17, Q-172): chuyến quá giờ về, ô
 * {@code OVERDUE_DOUBLE} là chuyến tới mốc gấp đôi.
 */
@Component
@Transactional(readOnly = true)
public class OverdueTripsMetric implements MetricSource<TripRecord> {

    public static final String CODE = "ACT.OVERDUE_TRIPS";

    private static final Metric<TripRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.ACTIVITIES,
            "Chuyến quá giờ về",
            "Số chuyến điểm danh về sau giờ về dự kiến hiện hành + CFG-M04-07 hoặc chưa điểm danh về khi qua mốc đó; ô riêng cho chuyến tới mốc giờ về + 2 × CFG-M04-07",
            "014 ActivityStatisticsApi.trips",
            new Metric.Computation.Count<>(r -> true),
            TripRecord::overdue,
            ActivityRecords::tripRow);

    private final ActivityRecords records;

    public OverdueTripsMetric(ActivityRecords records) {
        this.records = records;
    }

    @Override
    public Metric<TripRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<TripRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.trips(rough, from, to).filter(r -> r.overdue() != null);
    }
}
