package com.eldercare.backend.reporting.internal.meal.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.meal.service.MealSlipRecords.MealSlipRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code MEAL.DISCREPANCY_TICKETS} ({@code BUA.PHIEU_SAI_LECH}; FR-034, BR-M08-11): phiếu đã từng có sai lệch theo
 * 011, chia theo bữa.
 */
@Component
@Transactional(readOnly = true)
public class DiscrepancyTicketsMetric implements MetricSource<MealSlipRecord> {

    public static final String CODE = "MEAL.DISCREPANCY_TICKETS";

    private static final Metric<MealSlipRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.MEAL_TICKETS,
            "Phiếu bữa ăn có sai lệch",
            "Số phiếu đã từng ở Có sai lệch, kể cả bếp đã xử lý sau đó, ngày bữa trong khoảng, chia theo bữa; phần bổ sung không là phiếu riêng",
            "011 NutritionStatisticsApi.slips (hasDiscrepancy)",
            new Metric.Computation.Count<>(r -> true),
            MealSlipRecord::mealKey,
            MealSlipRecords::row);

    private final MealSlipRecords records;

    public DiscrepancyTicketsMetric(MealSlipRecords records) {
        this.records = records;
    }

    @Override
    public Metric<MealSlipRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<MealSlipRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(r -> r.view().hasDiscrepancy());
    }
}
