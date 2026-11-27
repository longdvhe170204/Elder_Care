package com.eldercare.backend.nutrition.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân tạo, sửa món (api-011 {@code DishCommand}; Q-445): tên, mô tả. */
@Schema(name = "DishCommand")
public record DishRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 200)
        String name,

        @Schema(maxLength = 2000) String description) {}
