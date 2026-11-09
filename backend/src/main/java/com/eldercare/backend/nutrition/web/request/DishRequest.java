package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.nutrition.api.dto.Texture;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Thân tạo, sửa món và nội dung bản sửa (api-011 {@code DishCommand}). */
@Schema(name = "DishCommand")
public record DishRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        String note,
        List<Long> ingredients,
        Boolean noCatalogIngredient,

        @ArraySchema(minItems = 1) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> suitableDietIds,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Texture texture) {}
