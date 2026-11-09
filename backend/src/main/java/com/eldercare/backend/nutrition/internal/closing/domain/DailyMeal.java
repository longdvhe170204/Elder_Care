package com.eldercare.backend.nutrition.internal.closing.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Bữa trong ngày (bữa danh mục + ngày; {@code bua_ngay}, gộp theo R-15 thành khóa kỹ thuật {@code M08.DAILY_MEAL}). Id suy
 * ra từ bữa và ngày ({@link #id(long, LocalDate)}), không có bảng; giờ bữa dự kiến đọc CFG-M08-04 tới khi chốt, sau đó là
 * giờ đã chốt vào lần chốt (FR-006 "đổi giờ bữa chỉ áp cho các bữa chưa chốt").
 */
public record DailyMeal(
        long id,
        long mealId,
        LocalDate date,
        String mealName,
        boolean servesResidential,
        boolean servesDayCare,
        Optional<Instant> mealTime,
        int lateThresholdMinutes,
        boolean closed) {

    private static final long DAYS = 100_000L;

    public static long id(long mealId, LocalDate date) {
        return mealId * DAYS + date.toEpochDay();
    }

    public static long mealIdOf(long dailyMealId) {
        return dailyMealId / DAYS;
    }

    public static LocalDate dateOf(long dailyMealId) {
        return LocalDate.ofEpochDay(dailyMealId % DAYS);
    }

    /** Mốc mở chốt: giờ bữa − CFG-M08-01 (FR-033). */
    public Optional<Instant> closingOpensAt(Duration lead) {
        return mealTime.map(t -> t.minus(lead));
    }

    public Instant lateDeadline() {
        return mealTime.orElseThrow().plus(Duration.ofMinutes(lateThresholdMinutes));
    }
}
