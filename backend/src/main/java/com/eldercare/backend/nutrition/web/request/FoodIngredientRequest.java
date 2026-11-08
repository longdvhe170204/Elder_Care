package com.eldercare.backend.nutrition.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân thêm thành phần thực phẩm (FR-003); 001 chưa có trường mô tả của dị nguyên nên {@code description} không lưu. */
@Schema(name = "FoodIngredient")
public record FoodIngredientRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, String description) {}
