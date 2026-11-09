package com.eldercare.backend.nutrition.internal.catalog.domain;

import com.eldercare.backend.common.data.CatalogStatus;

/** Món ăn đọc từ danh mục dùng chung; trạng thái vòng đời ở {@link DishAttributes#status()}. */
public record Dish(long id, String name, CatalogStatus catalogStatus, DishAttributes attributes, int version) {

    public DishStatus status() {
        return attributes.status();
    }
}
