package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Đổi món (api-011 {@code DishSwap}; FR-031, FR-032): thay, thêm, bớt món; đổi món thay thế. */
@Schema(name = "DishSwap")
public record DishSwapRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long mealId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long dietId,
        Long oldDishId,
        Long newDishId,
        List<Long> substituteDishIds,

        @Schema(
                implementation = String.class,
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = 2000)
        Reason reason,

        List<String> confirmedWarnings) {}
