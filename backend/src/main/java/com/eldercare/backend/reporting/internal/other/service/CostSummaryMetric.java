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
 * {@code COST.SUMMARY} ({@code CP.TONG_HOP}; FR-050, FR-051, FR-055): thành tiền các khoản trong khoảng theo chế độ đã chốt
 * hoặc tạm tính, chia theo loại chi phí; chi phí phát sinh của người cao tuổi, không phải báo cáo tài chính của viện.
 */
@Component
@Transactional(readOnly = true)
public class CostSummaryMetric implements MetricSource<CostRecord> {

    public static final String CODE = "COST.SUMMARY";

    private static final Metric<CostRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.INCURRED_COSTS,
            "Tổng chi phí phát sinh",
            "Thành tiền các khoản trong khoảng theo chế độ đã chốt hoặc tạm tính, chia theo loại chi phí; khoản thiếu đơn"
                    + " giá không cộng; chi phí phát sinh của người cao tuổi, không phải báo cáo tài chính của viện",
            "010 CostStatisticsApi.items",
            new Metric.Computation.Aggregate<>(Metric.Computation.Aggregate.Kind.SUM, CostRecord::amount),
            r -> r.view().costType().name(),
            CostItems::row);

    private final CostItems items;

    public CostSummaryMetric(CostItems items) {
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
