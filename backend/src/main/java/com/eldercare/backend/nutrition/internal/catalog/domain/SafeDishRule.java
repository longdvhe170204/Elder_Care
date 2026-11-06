package com.eldercare.backend.nutrition.internal.catalog.domain;

import com.eldercare.backend.common.error.UnmetCondition;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Món an toàn của chế độ ăn (FR-001, FR-005, FR-037a): món Hiệu lực, không chứa thành phần bị hạn chế của chế độ ăn.
 * Theo chủ dự án duyệt 2026-11-06: điều kiện "chế độ ăn phù hợp" không xét lúc lưu chế độ ăn (chế độ ăn mới chưa thể có
 * trong danh sách phù hợp của món), và khi chưa có món Hiệu lực nào thì chế độ ăn được lưu không có món an toàn.
 */
public final class SafeDishRule {

    /** Món được chọn làm món an toàn: trạng thái ({@code null} khi id không phải món) và thành phần đang hiệu lực. */
    public record SafeDish(DishStatus status, Set<Long> ingredients) {}

    private SafeDishRule() {}

    public static List<UnmetCondition> check(
            Optional<SafeDish> safeDish, Set<Long> dietRestrictions, boolean anyActiveDishExists) {
        if (safeDish.isEmpty()) {
            return anyActiveDishExists
                    ? List.of(new UnmetCondition("SAFE_DISH_REQUIRED", "Chế độ ăn phải có món an toàn (FR-001)"))
                    : List.of();
        }
        SafeDish dish = safeDish.get();
        if (dish.status() != DishStatus.ACTIVE) {
            return List.of(
                    new UnmetCondition("SAFE_DISH_NOT_ACTIVE", "Món an toàn phải ở trạng thái Hiệu lực (FR-004a)"));
        }
        if (dish.ingredients().stream().anyMatch(dietRestrictions::contains)) {
            return List.of(new UnmetCondition(
                    "SAFE_DISH_HAS_RESTRICTION", "Món an toàn chứa thành phần bị hạn chế của chế độ ăn (FR-005)"));
        }
        return List.of();
    }
}
