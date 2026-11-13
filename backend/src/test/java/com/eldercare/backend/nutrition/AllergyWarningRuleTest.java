package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.nutrition.internal.menu.domain.AllergyWarningRule;
import com.eldercare.backend.nutrition.internal.menu.domain.AllergyWarningRule.AllergyWarning;
import com.eldercare.backend.nutrition.internal.menu.domain.AllergyWarningRule.DietUser;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 011 T016 (BR-M08-07, FR-025 c, FR-020): món xung đột với ít nhất một người dùng chế độ ăn mà không có món thay thế phù
 * hợp với người đó thì cảnh báo, kèm danh sách người (bếp chỉ thấy số người).
 */
class AllergyWarningRuleTest {

    private static final LocalDate DAY = LocalDate.of(2025, 11, 10);
    private static final long DIET = 7;
    private static final long SHRIMP = 1;
    private static final long PEANUT = 2;
    private static final long SHRIMP_SOUP = 100;
    private static final long PLAIN_SOUP = 101;
    private static final long PEANUT_SOUP = 102;
    private static final Map<Long, Set<Long>> INGREDIENTS =
            Map.of(SHRIMP_SOUP, Set.of(SHRIMP), PLAIN_SOUP, Set.of(), PEANUT_SOUP, Set.of(PEANUT));

    @Test
    void BR_M08_07_suitableSubstituteForEveryone_isNotWarned() {
        List<MenuDish> dishes = List.of(new MenuDish(DAY, 1, DIET, SHRIMP_SOUP, List.of(PEANUT_SOUP, PLAIN_SOUP)));

        assertThat(AllergyWarningRule.find(
                        dishes, Map.of(DIET, List.of(user(1, SHRIMP, PEANUT))), Map.of(), INGREDIENTS))
                .isEmpty();
    }

    @Test
    void FR_020_otherAllergyNotAutoChecked_isIgnored() {
        List<MenuDish> dishes = List.of(new MenuDish(DAY, 1, DIET, SHRIMP_SOUP, List.of()));
        DietUser other = new DietUser(1, List.of(new AllergyItem(SHRIMP, true)), Set.of());

        assertThat(AllergyWarningRule.find(dishes, Map.of(DIET, List.of(other)), Map.of(), INGREDIENTS))
                .isEmpty();
    }

    @Test
    void FR_025_warningCode_isStableForSameSlotAndDish() {
        AllergyWarning a = new AllergyWarning(DAY, 1, DIET, SHRIMP_SOUP, List.of(1L));
        AllergyWarning b = new AllergyWarning(DAY, 1, DIET, SHRIMP_SOUP, List.of(1L, 2L));

        assertThat(a.code()).isEqualTo(b.code()).startsWith("ALLERGY:");
    }

    private static DietUser user(long id, long... allergens) {
        return new DietUser(
                id,
                java.util.Arrays.stream(allergens)
                        .mapToObj(a -> new AllergyItem(a, false))
                        .toList(),
                Set.of());
    }
}
