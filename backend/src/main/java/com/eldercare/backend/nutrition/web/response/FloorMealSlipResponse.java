package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.internal.slip.service.MealSlipService.FloorSlip;
import com.eldercare.backend.nutrition.web.response.KitchenMealSlipResponse.DietCountResponse;
import com.eldercare.backend.nutrition.web.response.KitchenMealSlipResponse.LabelResponse;
import com.eldercare.backend.nutrition.web.response.KitchenMealSlipResponse.SupplementResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * Phiếu cho nhân viên tại tầng (api-011 {@code FloorMealSlip}): trường của phiếu bếp, thêm người cần đối chiếu khi phục vụ
 * (FR-036a) và dị ứng (chỉ người có quyền xem dị ứng của người cao tuổi, 19.3).
 */
@Schema(name = "FloorMealSlip")
public record FloorMealSlipResponse(
        Long id,
        String floorArea,
        String meal,
        List<DietCountResponse> portionsByDiet,
        List<LabelResponse> labels,
        MealSlipStatus status,
        List<SupplementResponse> supplementaryPortions,

        @Schema(description = "Người có dị ứng loại khác (FR-036a)")
        List<Long> needsCrossCheck,

        @Schema(description = "Chỉ khi có quyền xem dị ứng") List<Map<String, Object>> allergies) {

    public static FloorMealSlipResponse of(FloorSlip s) {
        KitchenMealSlipResponse k = KitchenMealSlipResponse.of(s.slip());
        return new FloorMealSlipResponse(
                k.id(),
                k.floorArea(),
                k.meal(),
                k.portionsByDiet(),
                k.labels(),
                k.status(),
                k.supplementaryPortions(),
                s.needsCrossCheck(),
                s.allergies());
    }
}
