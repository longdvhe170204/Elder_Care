package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.slip.service.MealSlipService.KitchenSlip;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Phiếu cho bếp (api-011 {@code KitchenMealSlip}; FR-046, SC-008): chỉ tầng/khu, bữa, số suất theo chế độ ăn, nhãn suất
 * đặc biệt (họ tên, phòng, chế độ ăn, kết cấu, món, dấu đóng, đã chuẩn bị) và phần bổ sung. Không có dị ứng, bệnh lý,
 * thành phần xung đột, lý do món thay thế, danh sách cần đối chiếu. {@code labels[].id}, {@code supplementaryPortions} thêm
 * vào hợp đồng 2026-11-07 (chủ dự án chọn).
 */
@Schema(name = "KitchenMealSlip", description = "Chỉ trường FR-046; không có dị ứng, bệnh lý, thành phần xung đột")
public record KitchenMealSlipResponse(
        Long id,
        String floorArea,
        String meal,
        List<DietCountResponse> portionsByDiet,
        List<LabelResponse> labels,
        MealSlipStatus status,
        List<SupplementResponse> supplementaryPortions) {

    public record DietCountResponse(String diet, Integer portionCount) {}

    public record LabelResponse(
            Long id,
            String fullName,
            String room,
            String diet,
            Texture texture,
            String dishes,
            List<String> flags,
            Boolean prepared) {}

    public record SupplementResponse(
            Long id, Long dietId, Integer portionCount, Instant deliveredAt, Instant receivedAt) {}

    public static KitchenMealSlipResponse of(KitchenSlip s) {
        return new KitchenMealSlipResponse(
                s.id(),
                s.floorArea(),
                s.meal(),
                s.portionsByDiet().stream()
                        .map(c -> new DietCountResponse(c.diet(), c.portionCount()))
                        .toList(),
                s.labels().stream()
                        .map(l -> new LabelResponse(
                                l.id(),
                                l.fullName(),
                                l.room(),
                                l.diet(),
                                l.texture(),
                                l.dishes(),
                                l.flags(),
                                l.prepared()))
                        .toList(),
                s.status(),
                s.supplementaryPortions().stream()
                        .map(p -> new SupplementResponse(
                                p.id(), p.dietId(), p.portionCount(), p.deliveredAt(), p.receivedAt()))
                        .toList());
    }
}
