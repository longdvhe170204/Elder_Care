package com.eldercare.backend.nutrition.internal.catalog.domain;

import com.eldercare.backend.common.data.CatalogStatus;

/** Bữa ăn đọc từ danh mục dùng chung; thứ tự trong ngày theo {@code sortOrder}. */
public record Meal(long id, String name, CatalogStatus status, int sortOrder, MealAttributes attributes) {

    public boolean active() {
        return status == CatalogStatus.ACTIVE;
    }
}
