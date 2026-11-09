package com.eldercare.backend.reporting.internal.incident.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.resident.service.CurrentLocations;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code HEALTH.CRITICAL_FLAGS} ({@code SK.NGUY_KICH}; FR-020): số người cao tuổi đang mang dấu nguy kịch do 007
 * xác định, lúc xem; tầng, phòng theo nơi ở hiện tại.
 */
@Component
@Transactional(readOnly = true)
public class CriticalFlagsMetric implements MetricSource<CriticalFlagsMetric.CriticalRecord> {

    public static final String CODE = "HEALTH.CRITICAL_FLAGS";

    /** Một người cao tuổi đang mang dấu nguy kịch. */
    public record CriticalRecord(long resident, Long floorAreaId, Long roomId) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M05.CRITICAL:" + resident;
        }

        @Override
        public Instant referenceTime() {
            return null;
        }

        @Override
        public Long residentId() {
            return resident;
        }
    }

    private static final Metric<CriticalRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.CRITICAL_FLAGS,
            "Người đang nguy kịch",
            "Số người cao tuổi đang mang dấu nguy kịch lúc xem",
            "007 IncidentStatisticsApi.criticalResidentIds",
            new Metric.Computation.Count<>(r -> true),
            null,
            r -> new DetailRow(
                    r.sourceKey(),
                    "007",
                    "CRITICAL_FLAG",
                    null,
                    null,
                    r.floorAreaId(),
                    null,
                    r.residentId(),
                    null,
                    null,
                    Map.of(),
                    InfoGroup.HEALTH,
                    Map.of(),
                    false,
                    false,
                    false));

    private final ObjectProvider<IncidentStatisticsApi> statistics;
    private final ObjectProvider<ResidentLocationApi> locations;

    public CriticalFlagsMetric(
            ObjectProvider<IncidentStatisticsApi> statistics, ObjectProvider<ResidentLocationApi> locations) {
        this.statistics = statistics;
        this.locations = locations;
    }

    @Override
    public Metric<CriticalRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<CriticalRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        IncidentStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        List<Long> residents = api.criticalResidentIds(rough);
        Map<Long, LocationView> at = CurrentLocations.of(locations, residents);
        return residents.stream().map(id -> {
            LocationView l = at.get(id);
            return new CriticalRecord(id, l == null ? null : l.floorId(), l == null ? null : l.roomId());
        });
    }
}
