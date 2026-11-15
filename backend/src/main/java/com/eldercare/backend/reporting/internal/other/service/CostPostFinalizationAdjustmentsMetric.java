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
 * {@code COST.POST_FINALIZATION_ADJUSTMENTS} ({@code CP.DIEU_CHINH_SAU_CHOT}; FR-052, DBR-17): khoản điều chỉnh của kỳ đã
 * chốt tính vào kỳ của bảng chứa nó (khoảng báo cáo), chia theo kỳ gốc; số của kỳ gốc không đổi.
 */
@Component
@Transactional(readOnly = true)
public class CostPostFinalizationAdjustmentsMetric implements MetricSource<CostRecord> {

    public static final String CODE = "COST.POST_FINALIZATION_ADJUSTMENTS";

    private static final Metric<CostRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.INCURRED_COSTS,
            "Khoản điều chỉnh sau chốt",
            "Khoản điều chỉnh tính vào kỳ của bảng chứa nó, chia theo kỳ gốc",
            "010 CostStatisticsApi.items",
            new Metric.Computation.Count<>(
                    r -> "ADJUSTMENT".equals(r.view().origin()) && r.view().originalPeriod() != null),
            r -> r.view().originalPeriod() == null ? "NONE" : r.view().originalPeriod(),
            CostItems::row);

    private final CostItems items;

    public CostPostFinalizationAdjustmentsMetric(CostItems items) {
        this.items = items;
    }

    @Override
    public Metric<CostRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<CostRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return items.records(rough, from, to, filters)
                .filter(r -> "ADJUSTMENT".equals(r.view().origin()) && r.view().originalPeriod() != null);
    }
}
