package com.eldercare.backend.nutrition.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Nội dung thực đơn Nháp (api-011 {@code MenuContent}; FR-024). */
@Schema(name = "MenuContent")
public record MenuContentRequest(List<Row> dishes) {

    @Schema(name = "MenuContentDish")
    public record Row(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate date,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long mealId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long dietId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long dishId,

            List<Long> substituteDishIds) {}
}
