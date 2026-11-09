package com.eldercare.backend.nutrition.internal.closing.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Lần chốt suất (bảng {@code portion_closing}, nhóm 3; data-model mục 6). */
public record PortionClosing(
        long id,
        long dailyMealId,
        long mealId,
        LocalDate mealDate,
        Instant mealTime,
        int lateThresholdMinutes,
        long closedBy,
        Instant closedAt,
        boolean closedAfterMealTime,
        Long menuId,
        int totalPortions,
        List<CountedPerson> persons,
        List<FloorDietCount> portions,
        Set<String> flags,
        Set<String> missingData) {

    /** Số suất theo (tầng/khu, chế độ ăn) (FR-035; {@code suat_theo_che_do} gộp). */
    public record FloorDietCount(long floorAreaId, long dietId, int portionCount) {}

    public PortionClosing {
        persons = List.copyOf(persons);
        portions = List.copyOf(portions);
        flags = Set.copyOf(flags);
        missingData = Set.copyOf(missingData);
    }
}
