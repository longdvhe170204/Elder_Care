package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.care.api.spi.DailyMealPort;
import com.eldercare.backend.nutrition.api.MealScheduleApi;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 005 FR-055 (a), Q-440: giờ bữa của người cao tuổi cho "Lịch chăm sóc hằng ngày"; người có trong lịch bữa theo FR-042. */
@Service
class ResidentMealSlots implements DailyMealPort {
    private final DailyMeals dailyMeals;
    private final MealScheduleApi schedule;

    ResidentMealSlots(DailyMeals dailyMeals, MealScheduleApi schedule) {
        this.dailyMeals = dailyMeals;
        this.schedule = schedule;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealSlot> mealsOf(long residentId, LocalDate date) {
        return dailyMeals.on(date).stream()
                .filter(m -> m.mealTime().isPresent() && schedule.isInMealSchedule(m.id(), residentId))
                .map(m -> new MealSlot(m.mealTime().get(), m.mealName()))
                .toList();
    }
}
