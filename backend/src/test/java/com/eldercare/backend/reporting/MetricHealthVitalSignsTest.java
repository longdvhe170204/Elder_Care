package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.health.api.VitalSignStatisticsApi;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.api.dto.VitalSignReportView;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.vitalsign.service.VitalSignsMetric;
import com.eldercare.backend.reporting.internal.vitalsign.service.VitalSignsMetric.VitalSignRecord;
import com.eldercare.backend.support.fake.health.FakeVitalSignStatistics;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Chỉ tiêu chỉ số (tên logic {@code SK.CHI_SO}; 016 T108, FR-040) đọc {@code VitalSignStatisticsApi} của 007: số lần đo
 * đã xác nhận trong khoảng, chia theo mức lúc đo (vượt ngưỡng là Cảnh báo, Nguy hiểm theo 007 FR-081).
 */
class MetricHealthVitalSignsTest {

    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-08T00:00:00Z");

    private final FakeVitalSignStatistics statistics = new FakeVitalSignStatistics();
    private final VitalSignsMetric metric =
            new VitalSignsMetric(new StaticListableBeanFactory(Map.of("statistics", statistics))
                    .getBeanProvider(VitalSignStatisticsApi.class));

    private static VitalSignReportView reading(long id, long indicator, VitalSignLevel level, Instant at) {
        return new VitalSignReportView(id, 7, indicator, 101L, at, level);
    }

    @Test
    void FR_040_countReadingsInPeriod_byLevel_detailIsHealthInfo() {
        statistics.source(
                ScopeFilter.FACILITY,
                List.of(
                        reading(1, 1, VitalSignLevel.NORMAL, FROM),
                        reading(2, 1, VitalSignLevel.WARNING, FROM.plusSeconds(60)),
                        reading(3, 2, VitalSignLevel.DANGEROUS, FROM.plusSeconds(120)),
                        reading(4, 2, VitalSignLevel.WARNING, FROM.plusSeconds(180)),
                        reading(5, 3, VitalSignLevel.NO_THRESHOLD, FROM.plusSeconds(240)),
                        reading(6, 1, VitalSignLevel.DANGEROUS, TO)));
        Metric<VitalSignRecord> m = metric.metric();

        List<VitalSignRecord> included =
                m.included(metric.records(ScopeFilter.FACILITY, FROM, TO, ReportFilters.NONE), r -> true);
        MetricValue v = m.compute(included);

        assertThat(m.code()).isEqualTo("HEALTH.VITAL_SIGNS");
        assertThat(m.group()).isEqualTo(MetricGroup.HEALTH_EVENTS);
        assertThat(v.value()).isEqualByComparingTo("5");
        assertThat(v.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> b.value().intValue())
                .containsExactly(
                        Tuple.tuple("DANGEROUS", 1),
                        Tuple.tuple("NORMAL", 1),
                        Tuple.tuple("NO_THRESHOLD", 1),
                        Tuple.tuple("WARNING", 2));
        assertThat(m.details(included, "WARNING"))
                .allSatisfy(row -> {
                    assertThat(row.resultGroup()).isEqualTo(InfoGroup.HEALTH);
                    assertThat(row.floorAreaId()).isEqualTo(101L);
                    assertThat(row.residentId()).isEqualTo(7L);
                })
                .extracting(row -> row.result().get("indicatorId"))
                .containsExactly(1L, 2L);
    }
}
