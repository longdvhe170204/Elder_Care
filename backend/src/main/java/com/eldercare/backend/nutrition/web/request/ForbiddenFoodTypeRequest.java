package com.eldercare.backend.nutrition.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Thân thêm, ngừng loại đồ ăn bị cấm (api-011 {@code /nutrition/forbidden-food-types}; FR-007). */
public record ForbiddenFoodTypeRequest(
        @NotBlank String name,
        String description,
        @Schema(allowableValues = {"ACTIVE", "INACTIVE"}) String status) {}
