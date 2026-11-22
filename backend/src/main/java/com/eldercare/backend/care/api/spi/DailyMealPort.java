package com.eldercare.backend.care.api.spi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 005 FR-055 (a): 011 cài; bữa của một người cao tuổi trong ngày (giờ dự kiến, tên bữa), chỉ đọc. */
public interface DailyMealPort {
    List<MealSlot> mealsOf(long residentId, LocalDate date);

    record MealSlot(Instant at, String name) {}
}
