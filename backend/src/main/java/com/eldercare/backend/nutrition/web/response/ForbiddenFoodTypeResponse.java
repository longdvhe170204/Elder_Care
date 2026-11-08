package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.nutrition.internal.familyfood.domain.ForbiddenFoodType;

/** Loại đồ ăn bị cấm (FR-007): tên, mô tả, trạng thái {@code ACTIVE} hoặc {@code INACTIVE}. */
public record ForbiddenFoodTypeResponse(long id, String name, String description, String status) {

    public static ForbiddenFoodTypeResponse of(CatalogItem<ForbiddenFoodType> item) {
        return new ForbiddenFoodTypeResponse(
                item.id(),
                item.name(),
                item.attributes().description(),
                item.status().name());
    }
}
