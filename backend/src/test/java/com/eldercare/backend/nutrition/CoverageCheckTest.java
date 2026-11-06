package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck.DietInUse;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck.Gap;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck.MealServing;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T016 (BR-M08-06, FR-025 a, FR-027; tên {@code KiemTraDoPhuTest} trong tasks): mỗi chế độ ăn đang có người dùng có
 * món ở mọi bữa của mọi ngày mà bữa phục vụ loại lưu trú của người dùng; chế độ mặc định xét mọi bữa.
 */
class CoverageCheckTest {

    private static final LocalDate MONDAY = LocalDate.of(2025, 11, 10);
    private static final long BREAKFAST = 1;
    private static final long LUNCH = 2;
    private static final long DINNER = 3;
    private static final long DEFAULT_DIET = 10;
    private static final long DIABETIC = 11;
    private static final long DISH = 100;
    private static final List<MealServing> MEALS = List.of(
            new MealServing(BREAKFAST, true, false),
            new MealServing(LUNCH, true, true),
            new MealServing(DINNER, true, false));

    @Test
    void BR_M08_06_fullWeek_hasNoGap() {
        List<MenuDish> dishes = new ArrayList<>(fullWeek(DEFAULT_DIET));
        dishes.addAll(fullWeek(DIABETIC));

        assertThat(CoverageCheck.gaps(
                        MONDAY, dishes, List.of(defaultDiet(), new DietInUse(DIABETIC, false, true, false)), MEALS))
                .isEmpty();
    }

    @Test
    void BR_M08_06_missingFridayDinner_isListedPerDietDateMeal() {
        List<MenuDish> dishes = new ArrayList<>(fullWeek(DEFAULT_DIET));
        fullWeek(DIABETIC).stream()
                .filter(d -> !(d.date().equals(MONDAY.plusDays(4)) && d.mealId() == DINNER))
                .forEach(dishes::add);

        assertThat(CoverageCheck.gaps(
                        MONDAY, dishes, List.of(defaultDiet(), new DietInUse(DIABETIC, false, true, false)), MEALS))
                .containsExactly(new Gap(DIABETIC, MONDAY.plusDays(4), DINNER));
    }

    @Test
    void FR_027_dietWithOnlyDayCareUsers_needsOnlyMealsServingDayCare() {
        List<MenuDish> dishes = new ArrayList<>(fullWeek(DEFAULT_DIET));
        for (int day = 0; day < 7; day++) {
            dishes.add(dish(MONDAY.plusDays(day), LUNCH, DIABETIC));
        }

        assertThat(CoverageCheck.gaps(
                        MONDAY, dishes, List.of(defaultDiet(), new DietInUse(DIABETIC, false, false, true)), MEALS))
                .isEmpty();
    }

    @Test
    void FR_027_defaultDiet_isCheckedForEveryMeal() {
        List<MenuDish> dishes = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            dishes.add(dish(MONDAY.plusDays(day), LUNCH, DEFAULT_DIET));
        }

        assertThat(CoverageCheck.gaps(MONDAY, dishes, List.of(defaultDiet()), MEALS))
                .hasSize(14)
                .allMatch(gap -> gap.dietId() == DEFAULT_DIET && gap.mealId() != LUNCH);
    }

    @Test
    void FR_025_dietNotInUse_isNotChecked() {
        assertThat(CoverageCheck.gaps(MONDAY, fullWeek(DEFAULT_DIET), List.of(defaultDiet()), MEALS))
                .isEmpty();
    }

    @Test
    void FR_032_singleSlot_reportsOnlyThatSlot() {
        List<MenuDish> dishes = new ArrayList<>(fullWeek(DEFAULT_DIET));
        dishes.removeIf(d -> d.date().equals(MONDAY) && d.mealId() == BREAKFAST);
        dishes.removeIf(d -> d.date().equals(MONDAY.plusDays(1)) && d.mealId() == BREAKFAST);

        assertThat(CoverageCheck.gapsAt(MONDAY, BREAKFAST, dishes, List.of(defaultDiet()), MEALS))
                .containsExactly(new Gap(DEFAULT_DIET, MONDAY, BREAKFAST));
    }

    private static DietInUse defaultDiet() {
        return new DietInUse(DEFAULT_DIET, true, false, false);
    }

    private static List<MenuDish> fullWeek(long diet) {
        List<MenuDish> out = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            for (MealServing meal : MEALS) {
                out.add(dish(MONDAY.plusDays(day), meal.mealId(), diet));
            }
        }
        return out;
    }

    private static MenuDish dish(LocalDate date, long meal, long diet) {
        return new MenuDish(date, meal, diet, DISH, List.of());
    }
}
