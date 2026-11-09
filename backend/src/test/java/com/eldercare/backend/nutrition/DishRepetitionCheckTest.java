package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.internal.menu.domain.DishRepetitionCheck;
import com.eldercare.backend.nutrition.internal.menu.domain.DishRepetitionCheck.Repetition;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T016 (FR-026, BR-M08-07, CFG-M08-03; tên {@code LapMonTest} trong tasks): cùng món, cùng chế độ ăn từ N ngày liên tiếp,
 * không phân biệt bữa, tính qua ranh giới tuần; món thay thế không tính.
 */
class DishRepetitionCheckTest {

    private static final LocalDate MONDAY = LocalDate.of(2025, 11, 10);
    private static final long SOFT = 5;
    private static final long OTHER_DIET = 6;
    private static final long CHICKEN_PORRIDGE = 100;
    private static final long OTHER_DISH = 101;

    @Test
    void FR_026_threeConsecutiveDays_isWarned() {
        List<MenuDish> week = List.of(
                dish(MONDAY, 1, SOFT, CHICKEN_PORRIDGE),
                dish(MONDAY.plusDays(1), 1, SOFT, CHICKEN_PORRIDGE),
                dish(MONDAY.plusDays(2), 1, SOFT, CHICKEN_PORRIDGE));

        assertThat(DishRepetitionCheck.find(MONDAY, week, List.of(), 3))
                .containsExactly(new Repetition(SOFT, CHICKEN_PORRIDGE, MONDAY, MONDAY.plusDays(2)));
    }

    @Test
    void FR_026_twoDaysOrGap_isNotWarned() {
        List<MenuDish> week = List.of(
                dish(MONDAY, 1, SOFT, CHICKEN_PORRIDGE),
                dish(MONDAY.plusDays(1), 1, SOFT, CHICKEN_PORRIDGE),
                dish(MONDAY.plusDays(3), 1, SOFT, CHICKEN_PORRIDGE));

        assertThat(DishRepetitionCheck.find(MONDAY, week, List.of(), 3)).isEmpty();
    }

    private static MenuDish dish(LocalDate date, long meal, long diet, long dish) {
        return new MenuDish(date, meal, diet, dish, List.of());
    }
}
