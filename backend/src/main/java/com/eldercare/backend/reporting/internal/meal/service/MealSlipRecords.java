package com.eldercare.backend.reporting.internal.meal.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.NutritionStatisticsApi;
import com.eldercare.backend.nutrition.api.dto.MealSlipReportView;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiếu bữa ăn cho {@code MEAL.*} (FR-034) đọc {@code NutritionStatisticsApi.slips} của 011: dấu giao trễ (BR-M08-12,
 * CFG-M08-04) và "đã từng có sai lệch" (BR-M08-11) do 011 xác định; phần bổ sung không là phiếu riêng. Mốc xếp khoảng là
 * đầu ngày của bữa (giờ Việt Nam).
 */
@Component
@Transactional(readOnly = true)
public class MealSlipRecords {

    /** Một phiếu bữa ăn của một tầng/khu. */
    public record MealSlipRecord(MealSlipReportView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M08.MEAL_SLIP:" + view.slipId();
        }

        @Override
        public Instant referenceTime() {
            return view.date().atStartOfDay(BusinessTime.ZONE).toInstant();
        }

        @Override
        public Long floorAreaId() {
            return view.floorAreaId();
        }

        @Override
        public Long residentId() {
            return null;
        }

        String mealKey() {
            return Long.toString(view.mealId());
        }
    }

    private final ObjectProvider<NutritionStatisticsApi> statistics;

    public MealSlipRecords(ObjectProvider<NutritionStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    public Stream<MealSlipRecord> in(ScopeFilter rough, Instant from, Instant to) {
        NutritionStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        LocalDate first = LocalDate.ofInstant(from, BusinessTime.ZONE);
        LocalDate last = LocalDate.ofInstant(to.minusNanos(1), BusinessTime.ZONE);
        return api.slips(rough, first, last).map(MealSlipRecord::new);
    }

    static DetailRow row(MealSlipRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", r.view().date());
        result.put("mealId", r.view().mealId());
        result.put("deliveredAt", r.view().deliveredAt());
        result.put("late", r.view().late());
        result.put("hasDiscrepancy", r.view().hasDiscrepancy());
        return new DetailRow(
                r.sourceKey(),
                "011",
                "MEAL_SLIP",
                r.view().status().name(),
                r.referenceTime(),
                r.floorAreaId(),
                null,
                null,
                null,
                null,
                result,
                null,
                Map.of(),
                false,
                false,
                false);
    }
}
