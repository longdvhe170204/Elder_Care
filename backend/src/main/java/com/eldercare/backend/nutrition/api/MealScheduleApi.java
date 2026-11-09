package com.eldercare.backend.nutrition.api;

import com.eldercare.backend.nutrition.api.dto.DailyMealView;
import java.time.LocalDate;
import java.util.List;

/** Lịch bữa cho 005, 014 (module-interfaces mục 1.4). */
public interface MealScheduleApi {

    /** Bữa ngày, giờ dự kiến, đã chốt chưa. */
    List<DailyMealView> mealsOnDate(LocalDate date);

    /** 005 lập công việc BUA: người có trong lịch bữa này không. */
    boolean isInMealSchedule(long dailyMealId, long residentId);
}
