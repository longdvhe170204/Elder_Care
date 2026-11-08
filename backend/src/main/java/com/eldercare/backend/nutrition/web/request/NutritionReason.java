package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code requestBodies/Reason} của api-011. */
@Schema(name = "NutritionReason")
public record NutritionReason(
        @Schema(
                implementation = String.class,
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = 2000)
        Reason reason) {}
