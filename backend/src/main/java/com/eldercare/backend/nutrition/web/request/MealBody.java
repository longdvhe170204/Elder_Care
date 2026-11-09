package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.nutrition.internal.catalog.service.MealService.MealRow;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bữa ăn (api-011 {@code Meal}): một schema cho thân tạo, sửa và phần tử danh sách; giờ dự kiến, ngưỡng giao trễ chỉ đọc
 * (CFG-M08-04).
 */
@Schema(name = "Meal")
public record MealBody(
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        Boolean servesResidential,
        Boolean servesDayCare,
        Integer sortOrder,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) String expectedTime,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Integer lateDeliveryThresholdMinutes,
        String status) {

    public static MealBody of(MealRow row) {
        var m = row.item();
        return new MealBody(
                m.id(),
                m.code(),
                m.name(),
                m.attributes().servesResidential(),
                m.attributes().servesDayCare(),
                m.sortOrder(),
                row.expectedTime() == null ? null : row.expectedTime().toString(),
                row.lateDeliveryThresholdMinutes(),
                m.status().name());
    }
}
