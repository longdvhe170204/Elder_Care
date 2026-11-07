package com.eldercare.backend.nutrition.internal.menu.domain;

import java.time.LocalDate;
import java.util.List;

/** Một dòng nội dung thực đơn trong cột JSON của {@code menu} (FR-024, R-15). */
public record MenuDish(LocalDate date, long mealId, long dietId, long dishId, List<Long> substituteDishIds) {

    public MenuDish {
        substituteDishIds = substituteDishIds == null ? List.of() : List.copyOf(substituteDishIds);
    }
}
