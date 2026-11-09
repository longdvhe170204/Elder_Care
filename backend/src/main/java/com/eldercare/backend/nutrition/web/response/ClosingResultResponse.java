package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.closing.service.PortionClosingService.ClosingView;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;

/** Kết quả chốt hoặc tính sẵn (api-011 {@code ClosingResult}; FR-035): số suất theo tầng/khu, chế độ ăn; không có tên người. */
@Schema(name = "ClosingResult")
public record ClosingResultResponse(
        Long dailyMealId,
        List<FloorPortion> portionsByFloorArea,
        Integer specialPortions,
        Integer familyMealPortions,

        @ArraySchema(
                schema =
                        @Schema(
                                allowableValues = {
                                    "NO_MENU",
                                    "CLOSED_AFTER_MEAL_TIME",
                                    "MISSING_SUBSTITUTE_DISH",
                                    "NO_DIET_ASSIGNED"
                                }))
        Set<String> flags,

        @ArraySchema(schema = @Schema(allowableValues = {"STAY", "TRIP"}))
        Set<String> missingData) {

    @Schema(name = "ClosingFloorPortion")
    public record FloorPortion(Long floorAreaId, Long dietId, Integer portionCount) {}

    public static ClosingResultResponse of(ClosingView view) {
        return new ClosingResultResponse(
                view.dailyMealId(),
                view.portions().stream()
                        .map(p -> new FloorPortion(p.floorAreaId(), p.dietId(), p.portionCount()))
                        .toList(),
                view.specialPortions(),
                view.familyMealPortions(),
                view.flags(),
                view.missingData());
    }
}
