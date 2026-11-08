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
 * Chỉ tiêu {@code MEAL.LATE_TICKETS} ({@code BUA.PHIEU_TRE}; FR-034, BR-M08-12): phiếu giao trễ theo dấu của 011, chia theo
 * bữa.
 */
@Component
@Transactional(readOnly = true)
public class LateTicketsMetric implements MetricSource<MealSlipRecord> {

    public static final String CODE = "MEAL.LATE_TICKETS";

    private static final Metric<MealSlipRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.MEAL_TICKETS,
            "Phiếu bữa ăn giao trễ",
            "Số phiếu Đã giao sau giờ bữa + CFG-M08-04 hoặc chưa Đã giao khi tới mốc đó, ngày bữa trong khoảng, chia theo bữa; tầng/khu ở bộ lọc chung",
            "011 NutritionStatisticsApi.slips (late)",
            new Metric.Computation.Count<>(r -> true),
            MealSlipRecord::mealKey,
            MealSlipRecords::row);

    private final MealSlipRecords records;

    public LateTicketsMetric(MealSlipRecords records) {
        this.records = records;
    }

    @Override
    public Metric<MealSlipRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<MealSlipRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(r -> r.view().late());
    }
}
