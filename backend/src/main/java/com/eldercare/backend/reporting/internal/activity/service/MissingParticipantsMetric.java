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
 * Chỉ tiêu {@code ACT.MISSING_PARTICIPANTS} ({@code HD.THIEU_NGUOI}; FR-035, Q-173): chuyến có người thiếu khi về.
 */
@Component
@Transactional(readOnly = true)
public class MissingParticipantsMetric implements MetricSource<TripRecord> {

    public static final String CODE = "ACT.MISSING_PARTICIPANTS";

    private static final Metric<TripRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.ACTIVITIES,
            "Chuyến thiếu người",
            "Số chuyến có người thiếu khi điểm danh về (sự cố thiếu người), giờ về dự kiến trong khoảng; số người thiếu ở dòng chi tiết",
            "014 ActivityStatisticsApi.trips (missingOnReturn)",
            new Metric.Computation.Count<>(r -> true),
            null,
            ActivityRecords::tripRow);

    private final ActivityRecords records;

    public MissingParticipantsMetric(ActivityRecords records) {
        this.records = records;
    }

    @Override
    public Metric<TripRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<TripRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.trips(rough, from, to).filter(r -> r.view().missingOnReturn() > 0);
    }
}
