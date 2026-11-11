package com.eldercare.backend.reporting.internal.other.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.roombedallocation.api.BedStatisticsApi;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code BED.EMPTY_AWAITING_TRANSFER} ({@code GIUONG.TRONG_CHO_CHUYEN}; FR-020): giường Trống đang để dành cho người phải
 * chuyển khỏi giường mang dấu "chờ chuyển người" của 019, lúc xem; tầng theo phòng của giường (003).
 */
@Component
@Transactional(readOnly = true)
public class EmptyAwaitingTransferMetric implements MetricSource<EmptyAwaitingTransferMetric.BedRecord> {

    public static final String CODE = "BED.EMPTY_AWAITING_TRANSFER";

    public record BedRecord(long bedId, Long floorAreaId) implements MetricRecord {
        @Override
        public String sourceKey() {
            return "M03.BED:" + bedId;
        }

        @Override
        public Instant referenceTime() {
            return null;
        }

        @Override
        public Long residentId() {
            return null;
        }
    }

    private static final Metric<BedRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.BEDS,
            "Giường trống chờ chuyển người",
            "Giường Trống lúc xem được để dành cho người phải chuyển khỏi giường mang dấu chờ chuyển người (019)",
            "003 BedStatisticsApi.emptyAwaitingTransferBedIds",
            new Metric.Computation.Count<>(r -> true),
            null,
            r -> DetailRow.simple(r.sourceKey(), "003", "BED", "EMPTY", null, r.floorAreaId(), null, Map.of()));

    private final ObjectProvider<BedStatisticsApi> statistics;
    private final LocationStructureApi structure;
    private final BusinessTime time;

    public EmptyAwaitingTransferMetric(
            ObjectProvider<BedStatisticsApi> statistics, LocationStructureApi structure, BusinessTime time) {
        this.statistics = statistics;
        this.structure = structure;
        this.time = time;
    }

    @Override
    public Metric<BedRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<BedRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        BedStatisticsApi api = statistics.getIfAvailable();
        if (api == null) return Stream.empty();
        return api.emptyAwaitingTransferBedIds(rough, time.now()).stream()
                .sorted()
                .map(id -> new BedRecord(
                        id, structure.bed(id).map(b -> b.floorId()).orElse(null)));
    }
}
