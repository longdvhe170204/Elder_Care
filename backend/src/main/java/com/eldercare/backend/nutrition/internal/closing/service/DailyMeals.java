package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.internal.catalog.domain.Meal;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.closing.domain.DailyMeal;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing;
import com.eldercare.backend.nutrition.internal.closing.persistence.PortionClosingStore;
import com.eldercare.backend.nutrition.internal.menu.service.MealTimes;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bữa trong ngày (FR-033, FR-006): bữa Hiệu lực của danh mục tại một ngày; giờ bữa và ngưỡng giao trễ đọc CFG-M08-04 tới khi
 * chốt, sau chốt là giá trị đã ghi vào lần chốt. Mốc mở chốt theo CFG-M08-01; giờ hành chính CFG-M13-06 (FR-037a i).
 */
@Component
class DailyMeals {

    /** CFG-M13-06 (như {@code ShiftQueries.OfficeHours} của 008). */
    record OfficeHours(LocalTime from, LocalTime to, Set<DayOfWeek> days) {}

    private final NutritionCatalogStore catalog;
    private final MealTimes mealTimes;
    private final PortionClosingStore closings;
    private final ParameterApi parameters;

    DailyMeals(
            NutritionCatalogStore catalog, MealTimes mealTimes, PortionClosingStore closings, ParameterApi parameters) {
        this.catalog = catalog;
        this.mealTimes = mealTimes;
        this.closings = closings;
        this.parameters = parameters;
    }

    List<DailyMeal> on(LocalDate date) {
        return catalog.activeMeals().stream().map(meal -> of(meal, date)).toList();
    }

    /** 404 khi bữa không có trong danh mục. */
    DailyMeal require(long dailyMealId) {
        long mealId = DailyMeal.mealIdOf(dailyMealId);
        LocalDate date = DailyMeal.dateOf(dailyMealId);
        Meal meal;
        try {
            meal = catalog.meal(mealId);
        } catch (NotFoundException missing) {
            throw new NotFoundException();
        }
        return of(meal, date);
    }

    DailyMeal of(PortionClosing closing) {
        Meal meal = catalog.meal(closing.mealId());
        return new DailyMeal(
                closing.dailyMealId(),
                meal.id(),
                closing.mealDate(),
                meal.name(),
                meal.attributes().servesResidential(),
                meal.attributes().servesDayCare(),
                Optional.of(closing.mealTime()),
                closing.lateThresholdMinutes(),
                true);
    }

    Duration closingLead() {
        return parameters.read(ParameterCode.CFG_M08_01).asDuration();
    }

    Duration reminderLead() {
        return parameters.read(ParameterCode.CFG_M08_05).asDuration();
    }

    boolean officeHours(Instant at) {
        OfficeHours hours = parameters.read(ParameterCode.CFG_M13_06).asTable(OfficeHours.class);
        ZonedDateTime local = at.atZone(BusinessTime.ZONE);
        return hours.days().contains(local.getDayOfWeek())
                && !local.toLocalTime().isBefore(hours.from())
                && local.toLocalTime().isBefore(hours.to());
    }

    private DailyMeal of(Meal meal, LocalDate date) {
        long id = DailyMeal.id(meal.id(), date);
        Optional<PortionClosing> closing = closings.ofDailyMeal(id);
        if (closing.isPresent()) {
            return of(closing.get());
        }
        return new DailyMeal(
                id,
                meal.id(),
                date,
                meal.name(),
                meal.attributes().servesResidential(),
                meal.attributes().servesDayCare(),
                mealTimes.expectedAt(meal.id(), date),
                (int) mealTimes.lateThreshold().toMinutes(),
                false);
    }
}
