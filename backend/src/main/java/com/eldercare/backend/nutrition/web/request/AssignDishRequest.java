package com.eldercare.backend.nutrition.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân chỉ định món thay thế riêng (api-011 {@code POST /special-portions/{id}/assign-dish}; FR-037). */
@Schema(name = "AssignDishCommand")
public record AssignDishRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long dishId, String note) {}
