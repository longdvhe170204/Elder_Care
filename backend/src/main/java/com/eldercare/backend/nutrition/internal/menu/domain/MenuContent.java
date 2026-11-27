package com.eldercare.backend.nutrition.internal.menu.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Nội dung thực đơn chung gộp vào JSON (data-model mục 13.3). Món trùng (cùng ngày, bữa, món) chỉ giữ lần đầu: nội dung có
 * trước Q-445 chia theo chế độ ăn được đọc thành hợp các món của mọi chế độ ăn (data-model mục 13.6).
 */
public record MenuContent(List<MenuDish> dishes) {

    public MenuContent {
        dishes = dishes == null ? List.of() : List.copyOf(new ArrayList<>(new LinkedHashSet<>(dishes)));
    }

    /** Các món của một bữa ngày, theo thứ tự. */
    public List<Long> dishesOf(java.time.LocalDate date, long mealId) {
        return dishes.stream()
                .filter(d -> d.date().equals(date) && d.mealId() == mealId)
                .map(MenuDish::dishId)
                .toList();
    }
}
