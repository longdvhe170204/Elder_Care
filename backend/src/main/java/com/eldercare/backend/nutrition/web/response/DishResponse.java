package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** Món ăn (api-011 {@code Dish}; Q-445): tên, mô tả, url ảnh ({@code /api/v1/files/{id}}), trạng thái. */
@Schema(name = "Dish")
public record DishResponse(
        long id,
        String name,
        String description,
        String imageUrl,
        DishStatus status,
        String inactiveReason,
        int version) {

    public static DishResponse of(Dish dish) {
        return new DishResponse(
                dish.id(),
                dish.name(),
                dish.attributes().description(),
                imageUrl(dish.attributes().imageFileId()),
                dish.status(),
                dish.attributes().deactivationReason(),
                dish.version());
    }

    public static String imageUrl(Long fileId) {
        return fileId == null ? null : "/api/v1/files/" + fileId;
    }
}
