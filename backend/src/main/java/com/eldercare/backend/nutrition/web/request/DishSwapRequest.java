package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Đổi món (api-011 {@code DishSwap}; FR-031, Q-445): thay, thêm, bớt món của một bữa ngày. */
@Schema(name = "DishSwap")
public record DishSwapRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long mealId,
        Long oldDishId,
        Long newDishId,

        @Schema(
                implementation = String.class,
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = 2000)
        Reason reason) {}
