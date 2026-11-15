package com.eldercare.backend.reporting.internal.other.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code HEALTH.CONTAINMENT_ZONES} ({@code SK.KHOANH_VUNG}; FR-020): khu đang khoanh vùng lúc xem (007); một khu trên nhiều
 * tầng đếm một lần (trùng khóa nguồn, FR-015), người xem theo tầng thấy khu có tầng trong phạm vi.
 */
@Component
@Transactional(readOnly = true)
public class ContainmentZonesMetric implements MetricSource<ContainmentZonesMetric.ZoneRecord> {

    public static final String CODE = "HEALTH.CONTAINMENT_ZONES";

    public record ZoneRecord(long zoneId, Long floorAreaId, Instant startsAt) implements MetricRecord {
        @Override
        public String sourceKey() {
            return "M05.CONTAINMENT_ZONE:" + zoneId;
        }

        @Override
        public Instant referenceTime() {
            return startsAt;
        }

        @Override
        public Long residentId() {
            return null;
        }
    }

    private static final Metric<ZoneRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.CONTAINMENT_ZONES,
            "Khu đang khoanh vùng",
            "Khu đang khoanh vùng lúc xem; một khu trên nhiều tầng đếm một lần",
            "007 IncidentStatisticsApi.containmentZones",
            new Metric.Computation.Count<>(r -> true),
            null,
            r -> DetailRow.simple(
                    r.sourceKey(), "007", "CONTAINMENT_ZONE", "ACTIVE", r.startsAt(), r.floorAreaId(), null, Map.of()));

    private final ObjectProvider<IncidentStatisticsApi> statistics;

    public ContainmentZonesMetric(ObjectProvider<IncidentStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    @Override
    public Metric<ZoneRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<ZoneRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        IncidentStatisticsApi api = statistics.getIfAvailable();
        if (api == null) return Stream.empty();
        return api.containmentZones(rough).stream()
                .flatMap(z -> z.floorAreaIds().isEmpty()
                        ? Stream.of(new ZoneRecord(z.id(), null, z.startsAt()))
                        : z.floorAreaIds().stream().sorted().map(f -> new ZoneRecord(z.id(), f, z.startsAt())));
    }
}
