package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.internal.slip.service.MealSlipService.FloorSlip;
import com.eldercare.backend.nutrition.web.response.KitchenMealSlipResponse.SupplementResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Phiếu cho nhân viên tại tầng (api-011 {@code FloorMealSlip}; Q-445): trường của phiếu bếp, thêm suất giữ có tên (FR-040a).
 */
@Schema(name = "FloorMealSlip")
public record FloorMealSlipResponse(
        Long id,
        String group,
        String floorArea,
        String meal,
        List<String> dishes,
        Integer portionCount,
        Integer heldPortionCount,
        MealSlipStatus status,
        List<String> flags,
        List<SupplementResponse> supplementaryPortions,

        @Schema(description = "Suất giữ có tên cho người nhận tại tầng (FR-040a)")
        List<HeldPortionResponse> heldPortions) {

    @Schema(name = "FloorMealSlipHeldPortion")
    public record HeldPortionResponse(
            Long id,
            Long residentId,
            String fullName,

            @Schema(allowableValues = {"HELD", "SERVED", "DISCARDED"})
            String status) {}

    public static FloorMealSlipResponse of(FloorSlip s) {
        KitchenMealSlipResponse k = KitchenMealSlipResponse.of(s.slip());
        return new FloorMealSlipResponse(
                k.id(),
                k.group(),
                k.floorArea(),
                k.meal(),
                k.dishes(),
                k.portionCount(),
                k.heldPortionCount(),
                k.status(),
                k.flags(),
                k.supplementaryPortions(),
                s.heldPortions().stream()
                        .map(h -> new HeldPortionResponse(h.id(), h.residentId(), h.fullName(), h.status()))
                        .toList());
    }
}
