package com.eldercare.backend.nutrition.internal.menu.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hàm thuần kiểm tra độ phủ (BR-M08-06, FR-025 a, FR-027, FR-032; research M-07): mỗi chế độ ăn đang có người dùng phải có
 * ít nhất một món ở mọi bữa của mọi ngày trong tuần mà bữa phục vụ loại lưu trú của ít nhất một người dùng; chế độ ăn mặc
 * định xét mọi bữa (người thân ở lại dùng nó).
 */
public final class CoverageCheck {

    /** Bữa Hiệu lực trong danh mục và loại lưu trú được phục vụ (FR-006); thứ tự danh sách là thứ tự báo. */
    public record MealServing(long mealId, boolean servesResidential, boolean servesDayCare) {}

    /** Chế độ ăn đang có người dùng trong tuần và loại lưu trú của người dùng (FR-027). */
    public record DietInUse(long dietId, boolean isDefault, boolean residential, boolean dayCare) {

        boolean needs(MealServing meal) {
            return isDefault || (residential && meal.servesResidential()) || (dayCare && meal.servesDayCare());
        }
    }

    public record Gap(long dietId, LocalDate date, long mealId) {}

    private record Slot(LocalDate date, long mealId, long dietId) {}

    private CoverageCheck() {}

    public static List<Gap> gaps(
            LocalDate weekStart, List<MenuDish> dishes, List<DietInUse> diets, List<MealServing> meals) {
        Set<Slot> filled = filled(dishes);
        List<Gap> out = new ArrayList<>();
        for (DietInUse diet : diets) {
            for (int day = 0; day < 7; day++) {
                LocalDate date = weekStart.plusDays(day);
                for (MealServing meal : meals) {
                    if (diet.needs(meal) && !filled.contains(new Slot(date, meal.mealId(), diet.dietId()))) {
                        out.add(new Gap(diet.dietId(), date, meal.mealId()));
                    }
                }
            }
        }
        return out;
    }

    /** Chỉ một bữa của một ngày (Đổi món, FR-032). */
    public static List<Gap> gapsAt(
            LocalDate date, long mealId, List<MenuDish> dishes, List<DietInUse> diets, List<MealServing> meals) {
        Set<Slot> filled = filled(dishes);
        List<Gap> out = new ArrayList<>();
        meals.stream().filter(m -> m.mealId() == mealId).findFirst().ifPresent(meal -> {
            for (DietInUse diet : diets) {
                if (diet.needs(meal) && !filled.contains(new Slot(date, mealId, diet.dietId()))) {
                    out.add(new Gap(diet.dietId(), date, mealId));
                }
            }
        });
        return out;
    }

    private static Set<Slot> filled(List<MenuDish> dishes) {
        Set<Slot> out = new HashSet<>();
        dishes.forEach(d -> out.add(new Slot(d.date(), d.mealId(), d.dietId())));
        return out;
    }
}
