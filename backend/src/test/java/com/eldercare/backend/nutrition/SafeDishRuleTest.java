package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.SafeDishRule;
import com.eldercare.backend.nutrition.internal.catalog.domain.SafeDishRule.SafeDish;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 011 T008 (FR-001, FR-005, FR-037a): món an toàn phải Hiệu lực và không chứa hạn chế của chế độ ăn. Điều kiện "phù hợp"
 * không xét lúc lưu chế độ ăn; chưa có món Hiệu lực nào thì được để trống (chủ dự án duyệt 2026-11-06).
 */
class SafeDishRuleTest {

    private static final long SUGAR = 1;

    @Test
    void FR_001_activeDishWithoutRestrictedIngredient_passes() {
        assertThat(codes(SafeDishRule.check(
                        Optional.of(new SafeDish(DishStatus.ACTIVE, Set.of(2L))), Set.of(SUGAR), true)))
                .isEmpty();
    }

    @Test
    void FR_005_safeDishContainingRestriction_isBlocked() {
        assertThat(codes(SafeDishRule.check(
                        Optional.of(new SafeDish(DishStatus.ACTIVE, Set.of(SUGAR))), Set.of(SUGAR), true)))
                .containsExactly("SAFE_DISH_HAS_RESTRICTION");
    }

    @Test
    void FR_001_missingSafeDish_isBlockedWhenAnActiveDishExists() {
        assertThat(codes(SafeDishRule.check(Optional.empty(), Set.of(), true))).containsExactly("SAFE_DISH_REQUIRED");
    }

    private static java.util.List<String> codes(java.util.List<UnmetCondition> conditions) {
        return conditions.stream().map(UnmetCondition::code).toList();
    }
}
