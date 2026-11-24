package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.closing.domain.PortionContent;
import com.eldercare.backend.nutrition.internal.closing.service.PortionClosingService.ClosingView;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;

/**
 * Kết quả chốt hoặc tính sẵn (api-011 {@code ClosingResult}; FR-035, Q-445): tổng suất theo tầng/khu và phiếu nhân viên;
 * không có tên người.
 */
@Schema(name = "ClosingResult")
public record ClosingResultResponse(
        Long dailyMealId,
        List<GroupPortion> portionsByGroup,
        Integer staffPortions,
        Integer familyMealPortions,

        @ArraySchema(schema = @Schema(allowableValues = {"NO_MENU", "CLOSED_AFTER_MEAL_TIME"}))
        Set<String> flags,

        @ArraySchema(schema = @Schema(allowableValues = {"STAY", "TRIP", "SHIFT"}))
        Set<String> missingData) {

    @Schema(name = "ClosingGroupPortion")
    public record GroupPortion(
            @Schema(allowableValues = {"FLOOR", "STAFF"}) String group,
            @Schema(description = "Vắng khi group = STAFF") Long floorAreaId,
            Integer portionCount) {}

    public static ClosingResultResponse of(ClosingView view) {
        return new ClosingResultResponse(
                view.dailyMealId(),
                view.portions().stream()
                        .map(p -> p.floorAreaId() == PortionContent.STAFF_GROUP
                                ? new GroupPortion("STAFF", null, p.portionCount())
                                : new GroupPortion("FLOOR", p.floorAreaId(), p.portionCount()))
                        .toList(),
                view.portions().stream()
                        .filter(p -> p.floorAreaId() == PortionContent.STAFF_GROUP)
                        .mapToInt(p -> p.portionCount())
                        .sum(),
                view.familyMealPortions(),
                view.flags(),
                view.missingData());
    }
}
