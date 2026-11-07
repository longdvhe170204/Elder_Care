package com.eldercare.backend.nutrition.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.nutrition.api.dto.Texture;
import java.time.Instant;
import java.util.List;

/**
 * Phiên bản nội dung món ở {@code simple_record} loại {@code M08.DISH_VERSION}, cha {@code M08.DISH} (thay
 * {@code mon_an_phien_ban} và hai bảng con; data-model mục 0.1). Khóa dòng khi Bác sĩ xác nhận, trả lại hoặc rút (nhóm 3).
 */
public record DishVersionContent(
        List<Long> ingredients,
        boolean noCatalogIngredient,
        List<Long> suitableDietIds,
        Texture texture,
        Long submittedBy,
        Instant submittedAt,
        String returnReason,
        Long confirmedBy,
        Instant confirmedAt) {

    public static final SharedKind<DishVersionContent> KIND =
            SharedKind.of("M08.DISH_VERSION", DishVersionContent.class);

    public DishVersionContent {
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        suitableDietIds = suitableDietIds == null ? List.of() : List.copyOf(suitableDietIds);
    }

    public static DishVersionContent draft(
            List<Long> ingredients, boolean noCatalogIngredient, List<Long> suitableDietIds, Texture texture) {
        return new DishVersionContent(
                ingredients, noCatalogIngredient, suitableDietIds, texture, null, null, null, null, null);
    }

    public DishVersionContent submitted(long by, Instant at) {
        return new DishVersionContent(
                ingredients, noCatalogIngredient, suitableDietIds, texture, by, at, null, null, null);
    }

    public DishVersionContent returned(String reason) {
        return new DishVersionContent(
                ingredients,
                noCatalogIngredient,
                suitableDietIds,
                texture,
                submittedBy,
                submittedAt,
                reason,
                null,
                null);
    }

    public DishVersionContent confirmed(long by, Instant at) {
        return new DishVersionContent(
                ingredients, noCatalogIngredient, suitableDietIds, texture, submittedBy, submittedAt, null, by, at);
    }

    /** Cùng nội dung chuyên môn (thành phần, chế độ phù hợp, kết cấu) với phiên bản khác. */
    public boolean sameContent(DishVersionContent other) {
        return noCatalogIngredient == other.noCatalogIngredient
                && texture == other.texture
                && new java.util.HashSet<>(ingredients).equals(new java.util.HashSet<>(other.ingredients))
                && new java.util.HashSet<>(suitableDietIds).equals(new java.util.HashSet<>(other.suitableDietIds));
    }

    /** Điều kiện gửi xác nhận (FR-004): có thành phần hoặc dấu không có; ít nhất một chế độ ăn phù hợp. */
    public List<com.eldercare.backend.common.error.UnmetCondition> submitConditions() {
        java.util.List<com.eldercare.backend.common.error.UnmetCondition> out = new java.util.ArrayList<>();
        if (ingredients.isEmpty() && !noCatalogIngredient) {
            out.add(new com.eldercare.backend.common.error.UnmetCondition(
                    "INGREDIENTS_REQUIRED",
                    "Món phải có thành phần hoặc dấu không có thành phần trong danh mục (FR-004)"));
        }
        if (suitableDietIds.isEmpty()) {
            out.add(new com.eldercare.backend.common.error.UnmetCondition(
                    "SUITABLE_DIET_REQUIRED", "Món phải có ít nhất một chế độ ăn phù hợp (FR-004)"));
        }
        return out;
    }
}
