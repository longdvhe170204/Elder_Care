package com.eldercare.backend.nutrition.internal.conflict.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Hàm thuần đối chiếu xung đột của món với một người (FR-020, BR-M08-02, BR-M01-07; research M-06). Trùng theo id mục
 * danh mục dị nguyên; mục dị ứng loại "khác" (không kiểm tự động) và mục không có dị nguyên không so khớp.
 */
public final class ConflictCheck {

    /** Mục dị ứng Hiệu lực của người: {@code allergenId} null hoặc {@code notAutoChecked} thì không so khớp tự động. */
    public record AllergyItem(Long allergenId, boolean notAutoChecked) {}

    private ConflictCheck() {}

    public static Set<ConflictType> conflicts(
            Set<Long> dishIngredients,
            List<AllergyItem> activeAllergies,
            Set<Long> personalRestrictions,
            Set<Long> dietRestrictions) {
        Set<ConflictType> result = EnumSet.noneOf(ConflictType.class);
        if (activeAllergies.stream()
                .filter(a -> !a.notAutoChecked() && a.allergenId() != null)
                .anyMatch(a -> dishIngredients.contains(a.allergenId()))) {
            result.add(ConflictType.ALLERGY);
        }
        if (intersects(dishIngredients, personalRestrictions)) {
            result.add(ConflictType.PERSONAL_RESTRICTION);
        }
        if (intersects(dishIngredients, dietRestrictions)) {
            result.add(ConflictType.DIET_RESTRICTION);
        }
        return result;
    }

    public static boolean hasConflict(
            Set<Long> dishIngredients,
            List<AllergyItem> activeAllergies,
            Set<Long> personalRestrictions,
            Set<Long> dietRestrictions) {
        return !conflicts(dishIngredients, activeAllergies, personalRestrictions, dietRestrictions)
                .isEmpty();
    }

    private static boolean intersects(Set<Long> dishIngredients, Collection<Long> restrictions) {
        return restrictions.stream().anyMatch(dishIngredients::contains);
    }
}
