package com.eldercare.backend.nutrition.internal.familyfood.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Loại đồ ăn bị cấm theo chính sách của cơ sở (FR-007) ở {@code catalog_item} loại {@code M08.FORBIDDEN_FOOD_TYPE}; tên và
 * trạng thái ở cột chung, mô tả ở thuộc tính. Dùng đối chiếu đồ ăn gia đình (FR-053).
 */
public record ForbiddenFoodType(String description) {

    public static final SharedKind<ForbiddenFoodType> KIND =
            SharedKind.of("M08.FORBIDDEN_FOOD_TYPE", ForbiddenFoodType.class);
}
