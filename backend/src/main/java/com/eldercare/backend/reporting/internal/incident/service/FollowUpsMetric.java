package com.eldercare.backend.reporting.internal.incident.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.dto.MonitoringView;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code HEALTH.FOLLOW_UPS} ({@code SK.THEO_DOI}; FR-042, Q-307): trường hợp cần theo dõi đang Hiệu lực lúc xem,
 * gồm khoảng theo dõi sau ngã (007 FR-051) và theo dõi tiếp xúc, chia theo loại; tầng là tầng hiện tại do 007 trả.
 */
@Component
@Transactional(readOnly = true)
public class FollowUpsMetric implements MetricSource<FollowUpsMetric.FollowUpRecord> {

    public static final String CODE = "HEALTH.FOLLOW_UPS";

    /** Một khoảng theo dõi. */
    public record FollowUpRecord(MonitoringView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M05.MONITORING:" + view.type() + ":" + view.residentId() + ":" + view.startsAt();
        }

        @Override
        public Instant referenceTime() {
            return null;
        }

        @Override
        public Long floorAreaId() {
            return view.floorAreaId();
        }

        @Override
        public Long residentId() {
            return view.residentId();
        }
    }

    private static final Metric<FollowUpRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HEALTH_EVENTS,
            "Trường hợp cần theo dõi",
            "Khoảng theo dõi sau ngã và theo dõi tiếp xúc đang Hiệu lực lúc xem, chia theo loại",
            "007 IncidentStatisticsApi.underMonitoring",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().type(),
            r -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("startsAt", r.view().startsAt());
                result.put("endsAt", r.view().endsAt());
                return new DetailRow(
                        r.sourceKey(),
                        "007",
                        r.view().type(),
                        null,
                        null,
                        r.floorAreaId(),
                        null,
                        r.residentId(),
                        null,
                        null,
                        result,
                        InfoGroup.HEALTH,
                        Map.of(),
                        false,
                        false,
                        false);
            });

    private final ObjectProvider<IncidentStatisticsApi> statistics;

    public FollowUpsMetric(ObjectProvider<IncidentStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    @Override
    public Metric<FollowUpRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<FollowUpRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        IncidentStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        return api.underMonitoring(rough).stream().map(FollowUpRecord::new);
    }
}
