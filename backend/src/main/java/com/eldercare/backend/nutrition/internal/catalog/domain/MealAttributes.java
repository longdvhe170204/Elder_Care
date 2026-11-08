package com.eldercare.backend.nutrition.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Thuộc tính của bữa ăn ở {@code catalog_item} loại {@code M08.MEAL} (FR-006; data-model mục 0.1, 3; api-011 {@code Meal}):
 * loại lưu trú được phục vụ. Giờ bữa dự kiến, ngưỡng giao trễ là tham số CFG-M08-04. Lệnh quản lý ở {@code MealService}
 * (Q-401); thực đơn chỉ đọc.
 */
public record MealAttributes(boolean servesResidential, boolean servesDayCare) {

    public static final SharedKind<MealAttributes> KIND =
            SharedKind.of("M08.MEAL", MealAttributes.class).notDeletable();
}
