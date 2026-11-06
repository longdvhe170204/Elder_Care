package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.nutrition.api.MealScheduleApi;
import com.eldercare.backend.nutrition.api.dto.DailyMealView;
import com.eldercare.backend.nutrition.internal.closing.domain.DailyMeal;
import com.eldercare.backend.nutrition.internal.closing.persistence.PortionClosingStore;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lịch bữa cho 005, 014 (FR-042; module-interfaces mục 1.4; research M-08): bữa trong ngày có giờ dự kiến; người có trong
 * lịch bữa theo lần chốt và phát sinh, chưa chốt thì theo tính sẵn tại lúc hỏi. Bản cài {@code TaskSourcePort} loại
 * {@code MEAL} và báo bữa bị bỏ vì vắng ({@code TaskSourceApi.sourceCancelled}) chờ 005 (team-plan mục 8).
 */
@Service
class MealScheduleQueries implements MealScheduleApi {

    private final DailyMeals dailyMeals;
    private final PortionClosingStore closings;
    private final PortionClosingService closing;
    private final PortionCounter counter;

    MealScheduleQueries(
            DailyMeals dailyMeals,
            PortionClosingStore closings,
            PortionClosingService closing,
            PortionCounter counter) {
        this.dailyMeals = dailyMeals;
        this.closings = closings;
        this.closing = closing;
        this.counter = counter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyMealView> mealsOnDate(LocalDate date) {
        return dailyMeals.on(date).stream()
                .filter(m -> m.mealTime().isPresent())
                .map(m -> new DailyMealView(
                        m.id(), m.mealId(), m.date(), m.mealTime().get(), m.closed()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInMealSchedule(long dailyMealId, long residentId) {
        DailyMeal meal;
        try {
            meal = dailyMeals.require(dailyMealId);
        } catch (com.eldercare.backend.common.error.NotFoundException missing) {
            return false;
        }
        if (meal.mealTime().isEmpty()) {
            return false;
        }
        if (meal.closed()) {
            return closings.ofDailyMeal(dailyMealId)
                    .map(c -> closing.current(c).containsKey("RESIDENT:" + residentId))
                    .orElse(false);
        }
        return counter.count(meal, Optional.of(Set.of(residentId)), Map.of(), false).persons().stream()
                .anyMatch(p -> p.residentId() == residentId && p.familyMemberId() == null);
    }
}
