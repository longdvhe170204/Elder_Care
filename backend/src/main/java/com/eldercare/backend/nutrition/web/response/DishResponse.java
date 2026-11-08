package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishVersionContent;
import com.eldercare.backend.nutrition.internal.catalog.service.DishService.DishDetail;
import com.eldercare.backend.nutrition.web.request.DishRequest;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Món ăn (api-011 {@code Dish}): {@code effective} là nội dung đã xác nhận đang dùng; {@code pendingRevision} là nội dung
 * đang soạn hoặc chờ xác nhận (bản sửa của món Hiệu lực, hoặc nội dung của món chưa từng xác nhận).
 */
@Schema(name = "Dish")
public record DishResponse(
        long id, String name, DishStatus status, DishRequest effective, DishRequest pendingRevision, int version) {

    public static DishResponse of(DishDetail detail) {
        var dish = detail.dish();
        return new DishResponse(
                dish.id(),
                dish.name(),
                dish.status(),
                body(dish.name(), dish.attributes().note(), detail.effective()),
                body(dish.name(), dish.attributes().note(), detail.pending()),
                dish.version());
    }

    private static DishRequest body(String name, String note, DishVersionContent content) {
        return content == null
                ? null
                : new DishRequest(
                        name,
                        note,
                        content.ingredients(),
                        content.noCatalogIngredient(),
                        content.suitableDietIds(),
                        content.texture());
    }
}
