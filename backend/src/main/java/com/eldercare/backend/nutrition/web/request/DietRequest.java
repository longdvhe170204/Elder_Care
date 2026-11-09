package com.eldercare.backend.nutrition.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Thân tạo, sửa chế độ ăn (api-011 {@code DietCommand}). */
@Schema(name = "DietCommand")
public record DietRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        String description,
        Boolean treatmentRelated,
        Boolean isDefault,
        List<Long> restrictions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long safeDishId) {}
