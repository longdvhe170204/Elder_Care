package com.eldercare.backend.reporting.internal.other.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.other.service.CostItems.CostRecord;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code COST.EVENT_GENERATED_RATE} ({@code CP.TY_LE_THEO_SU_KIEN}; FR-053, Q-306): tỷ lệ khoản có nguồn sinh "theo sự
 * kiện" trên mọi khoản trong khoảng, theo trường nguồn sinh của 010 FR-014; chia theo nguồn sinh.
 */
@Component
@Transactional(readOnly = true)
public class CostEventGeneratedRateMetric implements MetricSource<CostRecord> {

    public static final String CODE = "COST.EVENT_GENERATED_RATE";

    private static final Metric<CostRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.INCURRED_COSTS,
            "Tỷ lệ khoản lập theo sự kiện",
            "Khoản có nguồn sinh theo sự kiện trên mọi khoản trong khoảng (theo sự kiện, nhập tay, mua hộ, điều chỉnh)",
            "010 CostStatisticsApi.items",
            new Metric.Computation.Ratio<>(
                    r -> true, r -> "EVENT_BASED".equals(r.view().origin())),
            r -> r.view().origin(),
            CostItems::row);

    private final CostItems items;

    public CostEventGeneratedRateMetric(CostItems items) {
        this.items = items;
    }

    @Override
    public Metric<CostRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<CostRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return items.records(rough, from, to, filters);
    }
}
