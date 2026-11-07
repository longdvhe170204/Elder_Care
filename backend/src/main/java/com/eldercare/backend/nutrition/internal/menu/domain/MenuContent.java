package com.eldercare.backend.nutrition.internal.menu.domain;

import java.util.List;

/** Nội dung thực đơn gộp từ hai bảng con cũ vào JSON (data-model mục 0.1, 5). */
public record MenuContent(List<MenuDish> dishes) {

    public MenuContent {
        dishes = dishes == null ? List.of() : List.copyOf(dishes);
    }
}
