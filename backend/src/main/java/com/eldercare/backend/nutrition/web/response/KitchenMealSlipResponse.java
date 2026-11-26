package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.internal.slip.service.MealSlipService.KitchenSlip;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Phiếu cho bếp (api-011 {@code KitchenMealSlip}; FR-046, SC-008, Q-445): tầng/khu hoặc phiếu nhân viên, các món của bữa,
 * tổng suất, số suất giữ; không tên, dị ứng, chế độ ăn, bệnh lý.
 */
@Schema(name = "KitchenMealSlip")
public record KitchenMealSlipResponse(
        Long id,
        @Schema(allowableValues = {"FLOOR", "STAFF"}) String group,
        @Schema(description = "Vắng khi group = STAFF") String floorArea,
        String meal,
        List<String> dishes,
        Integer portionCount,

        @Schema(description = "Suất giữ (FR-040a), không kèm tên")
        Integer heldPortionCount,

        MealSlipStatus status,

        @ArraySchema(schema = @Schema(allowableValues = {"NO_MENU", "CLOSED_AFTER_MEAL_TIME"}))
        List<String> flags,

        List<SupplementResponse> supplementaryPortions) {

    @Schema(name = "KitchenMealSlipSupplement")
    public record SupplementResponse(Long id, Integer portionCount, Instant deliveredAt, Instant receivedAt) {}

    public static KitchenMealSlipResponse of(KitchenSlip s) {
        return new KitchenMealSlipResponse(
                s.id(),
                s.staff() ? "STAFF" : "FLOOR",
                s.floorArea(),
                s.meal(),
                s.dishes(),
                s.portionCount(),
                s.heldPortionCount(),
                s.status(),
                s.flags(),
                s.supplementaryPortions().stream()
                        .map(p -> new SupplementResponse(p.id(), p.portionCount(), p.deliveredAt(), p.receivedAt()))
                        .toList());
    }
}
