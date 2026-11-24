package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.closing.service.MealPlanning.PlanningInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Số suất dự kiến và số người cao tuổi theo loại dị ứng của một ngày, bữa (api-011 {@code MealPlanningInfo}; FR-025a):
 * không tên, phòng, tầng.
 */
@Schema(name = "MealPlanningInfo")
public record MealPlanningInfoResponse(
        LocalDate date,
        long mealId,
        int residentPortions,
        int familyPortions,
        int staffPortions,
        List<AllergyCount> allergyCounts) {

    @Schema(name = "MealPlanningAllergyCount")
    public record AllergyCount(
            @Schema(description = "Vắng với nhóm dị ứng khác")
            Long allergenId,

            String name,
            int residentCount) {}

    public static MealPlanningInfoResponse of(PlanningInfo info) {
        return new MealPlanningInfoResponse(
                info.date(),
                info.mealId(),
                info.residentPortions(),
                info.familyPortions(),
                info.staffPortions(),
                info.allergyCounts().stream()
                        .map(a -> new AllergyCount(a.allergenId(), a.name(), a.residentCount()))
                        .toList());
    }
}
