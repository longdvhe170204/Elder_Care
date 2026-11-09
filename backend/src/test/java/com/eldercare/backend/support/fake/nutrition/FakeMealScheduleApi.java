package com.eldercare.backend.support.fake.nutrition;

import com.eldercare.backend.nutrition.api.MealScheduleApi;
import com.eldercare.backend.nutrition.api.dto.DailyMealView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bản giả {@link MealScheduleApi} cho 005, 014: bữa ngày và người có trong lịch bữa do test đặt. */
public class FakeMealScheduleApi implements MealScheduleApi {

    private final List<DailyMealView> meals = new ArrayList<>();
    private final Set<String> members = new HashSet<>();

    public FakeMealScheduleApi meal(DailyMealView meal) {
        meals.add(meal);
        return this;
    }

    public FakeMealScheduleApi member(long dailyMealId, long residentId) {
        members.add(dailyMealId + ":" + residentId);
        return this;
    }

    @Override
    public List<DailyMealView> mealsOnDate(LocalDate date) {
        return meals.stream().filter(m -> m.date().equals(date)).toList();
    }

    @Override
    public boolean isInMealSchedule(long dailyMealId, long residentId) {
        return members.contains(dailyMealId + ":" + residentId);
    }
}
