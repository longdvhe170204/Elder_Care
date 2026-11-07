package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.NutritionStatisticsApi;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodReportView;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodStatus;
import com.eldercare.backend.nutrition.api.dto.MealSlipReportView;
import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.api.dto.PortionReportView;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.MealSlip;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing;
import com.eldercare.backend.nutrition.internal.closing.persistence.ClosingRecordStore;
import com.eldercare.backend.nutrition.internal.closing.persistence.PortionClosingStore;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFood;
import com.eldercare.backend.nutrition.internal.familyfood.persistence.FamilyFoodStore;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Số liệu dinh dưỡng cho báo cáo 016 (module-interfaces mục 1.5): đọc lần chốt, phiếu, phát sinh, đồ ăn gia đình của chính
 * 011, không ghi và không đổi dữ liệu. Số suất và phiếu lọc theo tầng/khu của phạm vi; đồ ăn gia đình lọc theo người cao
 * tuổi hoặc tầng/phòng nơi người đó ở lúc ghi nhận.
 */
@Service
@Transactional(readOnly = true)
class NutritionStatisticsService implements NutritionStatisticsApi {

    private final PortionClosingStore closings;
    private final ClosingRecordStore records;
    private final FamilyFoodStore foods;
    private final ResidentLocationApi locations;
    private final BusinessTime time;

    NutritionStatisticsService(
            PortionClosingStore closings,
            ClosingRecordStore records,
            FamilyFoodStore foods,
            ResidentLocationApi locations,
            BusinessTime time) {
        this.closings = closings;
        this.records = records;
        this.foods = foods;
        this.locations = locations;
        this.time = time;
    }

    @Override
    public Stream<MealSlipReportView> slips(ScopeFilter scope, LocalDate from, LocalDate to) {
        Instant now = time.now();
        List<MealSlipReportView> result = new ArrayList<>();
        for (PortionClosing closing : closings.mealDateBetween(from, to)) {
            Instant deadline = closing.mealTime().plus(Duration.ofMinutes(closing.lateThresholdMinutes()));
            for (SimpleRecord<MealSlip> slip : records.slips(closing.dailyMealId())) {
                MealSlip content = slip.content();
                if (!inFloor(scope, content.floorAreaId())) {
                    continue;
                }
                result.add(new MealSlipReportView(
                        slip.id(),
                        closing.mealDate(),
                        closing.mealId(),
                        content.floorAreaId(),
                        MealSlipStatus.valueOf(slip.status()),
                        content.deliveredAt(),
                        isLate(closing, content, deadline, now),
                        !records.discrepancies(slip.id()).isEmpty()));
            }
        }
        return result.stream();
    }

    @Override
    public Stream<PortionReportView> portions(ScopeFilter scope, LocalDate from, LocalDate to) {
        List<PortionReportView> result = new ArrayList<>();
        for (PortionClosing closing : closings.mealDateBetween(from, to)) {
            closing.portions().stream()
                    .filter(p -> inFloor(scope, p.floorAreaId()))
                    .forEach(p -> result.add(new PortionReportView(
                            closing.mealDate(),
                            closing.mealId(),
                            p.floorAreaId(),
                            p.dietId(),
                            p.portionCount(),
                            false)));
            records.adjustments(closing.dailyMealId()).stream()
                    .map(event -> event.content())
                    .filter(a -> inFloor(scope, a.floorAreaId()))
                    .forEach(a -> result.add(new PortionReportView(
                            closing.mealDate(),
                            closing.mealId(),
                            a.floorAreaId(),
                            a.dietId(),
                            a.portionCount(),
                            true)));
        }
        return result.stream();
    }

    @Override
    public Stream<FamilyFoodReportView> familyFoods(ScopeFilter scope, LocalDate from, LocalDate to) {
        Instant start = time.startOfDay(from);
        Instant end = time.startOfDay(to.plusDays(1));
        return foods.all().stream()
                .filter(row -> {
                    Instant at = row.content().recordedAt();
                    return !at.isBefore(start) && at.isBefore(end) && inScope(scope, row);
                })
                .map(row -> new FamilyFoodReportView(
                        row.id(),
                        row.residentId(),
                        FamilyFoodStatus.valueOf(row.status()),
                        row.content().recordedAt()));
    }

    /** Phiếu trễ: giao sau hạn, hoặc quá hạn mà chưa giao; không áp cho lần chốt sau giờ bữa (FR-048a, BR-M08-12). */
    private static boolean isLate(PortionClosing closing, MealSlip slip, Instant deadline, Instant now) {
        if (closing.closedAfterMealTime()) {
            return false;
        }
        return slip.deliveredAt() == null
                ? now.isAfter(deadline)
                : slip.deliveredAt().isAfter(deadline);
    }

    private static boolean inFloor(ScopeFilter scope, long floorAreaId) {
        return scope.facilityWide() || scope.floorAreaIds().contains(floorAreaId);
    }

    private boolean inScope(ScopeFilter scope, SimpleRecord<FamilyFood> row) {
        if (scope.facilityWide() || scope.residentIds().contains(row.residentId())) {
            return true;
        }
        return locations.at(row.residentId(), row.content().recordedAt()).stream()
                .anyMatch(l -> (l.floorId() != null && scope.floorAreaIds().contains(l.floorId()))
                        || (l.roomId() != null && scope.roomIds().contains(l.roomId())));
    }
}
