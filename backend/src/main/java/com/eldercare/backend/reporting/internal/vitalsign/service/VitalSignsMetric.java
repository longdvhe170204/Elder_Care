package com.eldercare.backend.reporting.internal.vitalsign.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.health.api.VitalSignStatisticsApi;
import com.eldercare.backend.health.api.dto.VitalSignReportView;
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
 * Chỉ tiêu {@code HEALTH.VITAL_SIGNS} ({@code SK.CHI_SO}; FR-040): số lần đo đã xác nhận trong khoảng theo 007 FR-081
 * (bản đo lại thay bản đầu, mức lúc đo theo ngưỡng hiệu lực), chia theo mức; vượt ngưỡng là ô Cảnh báo, Nguy hiểm. Loại
 * chỉ số ở dòng chi tiết; tầng theo bộ lọc chung.
 */
@Component
@Transactional(readOnly = true)
public class VitalSignsMetric implements MetricSource<VitalSignsMetric.VitalSignRecord> {

    public static final String CODE = "HEALTH.VITAL_SIGNS";

    /** Một lần đo; mốc xếp khoảng là lúc đo. */
    public record VitalSignRecord(VitalSignReportView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M06.VITAL_SIGN:" + view.recordId();
        }

        @Override
        public Instant referenceTime() {
            return view.measuredAt();
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

    private static final Metric<VitalSignRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HEALTH_EVENTS,
            "Lần đo chỉ số",
            "Số lần đo đã xác nhận trong khoảng, chia theo mức lúc đo (Bình thường, Cảnh báo, Nguy hiểm, chưa có ngưỡng)",
            "007 VitalSignStatisticsApi.readings",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().level().name(),
            r -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("indicatorId", r.view().indicatorId());
                result.put("level", r.view().level().name());
                return new DetailRow(
                        r.sourceKey(),
                        "007",
                        "VITAL_SIGN",
                        r.view().level().name(),
                        r.referenceTime(),
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

    private final ObjectProvider<VitalSignStatisticsApi> statistics;

    public VitalSignsMetric(ObjectProvider<VitalSignStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    @Override
    public Metric<VitalSignRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<VitalSignRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        VitalSignStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        return api.readings(rough, from, to).map(VitalSignRecord::new);
    }
}
