package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson.DishChoice;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionContent;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionContent.Input;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionContent.Portion;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionFlag;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.LongPredicate;
import org.junit.jupiter.api.Test;

/**
 * 011 T021 (FR-036, FR-036a, FR-037, FR-037a, FR-038): suất đặc biệt khi món xung đột, kết cấu khác thường, có chỉ định
 * riêng; món thay thế đầu tiên không xung đột; thiếu món thay thế; món an toàn chỉ khi được phép áp và không xung đột.
 */
class PortionContentTest {

    private static final LocalDate DAY = LocalDate.of(2025, 11, 12);
    private static final long SHRIMP_SOUP = 1;
    private static final long PEANUT_SOUP = 2;
    private static final long PLAIN_SOUP = 3;
    private static final long SAFE = 9;
    private static final LongPredicate SHRIMP_AND_PEANUT = id -> id == SHRIMP_SOUP || id == PEANUT_SOUP;
    private static final LongPredicate NONE = id -> false;

    private static Input input(
            List<MenuDish> rows, Texture texture, Long assigned, LongPredicate conflicts, boolean applySafe) {
        return new Input(rows, true, texture, assigned, conflicts, SAFE, applySafe, false);
    }

    private static MenuDish row(long dish, Long... substitutes) {
        return new MenuDish(DAY, 1, 5, dish, List.of(substitutes));
    }

    @Test
    void FR_036_noConflict_regularTexture_isNormalPortion() {
        Portion p = PortionContent.of(input(List.of(row(PLAIN_SOUP)), Texture.REGULAR, null, NONE, false));

        assertThat(p.special()).isFalse();
        assertThat(p.flags()).isEmpty();
        assertThat(p.dishes()).containsExactly(new DishChoice(PLAIN_SOUP, null));
    }

    @Test
    void FR_036_conflict_picksFirstNonConflictingSubstitute() {
        Portion p = PortionContent.of(input(
                List.of(row(SHRIMP_SOUP, PEANUT_SOUP, PLAIN_SOUP)), Texture.REGULAR, null, SHRIMP_AND_PEANUT, false));

        assertThat(p.special()).isTrue();
        assertThat(p.dishes()).containsExactly(new DishChoice(SHRIMP_SOUP, PLAIN_SOUP));
        assertThat(p.flags()).isEmpty();
    }

    @Test
    void FR_036_conflictWithoutSuitableSubstitute_isMarkedMissing() {
        Portion p = PortionContent.of(
                input(List.of(row(SHRIMP_SOUP, PEANUT_SOUP)), Texture.REGULAR, null, SHRIMP_AND_PEANUT, false));

        assertThat(p.special()).isTrue();
        assertThat(p.flags()).containsExactly(PortionFlag.MISSING_SUBSTITUTE_DISH);
        assertThat(p.dishes()).containsExactly(new DishChoice(SHRIMP_SOUP, null));
    }

    @Test
    void FR_036_nonRegularTexture_orAssignedDish_isSpecial() {
        assertThat(PortionContent.of(input(List.of(row(PLAIN_SOUP)), Texture.PUREED, null, NONE, false))
                        .special())
                .isTrue();
        Portion assigned = PortionContent.of(
                input(List.of(row(SHRIMP_SOUP)), Texture.REGULAR, PLAIN_SOUP, SHRIMP_AND_PEANUT, false));
        assertThat(assigned.special()).isTrue();
        assertThat(assigned.flags()).isEmpty();
        assertThat(assigned.dishes()).containsExactly(new DishChoice(SHRIMP_SOUP, PLAIN_SOUP));
    }

    @Test
    void FR_037a_safeDish_appliedOnlyWhenAllowed_andNotConflicting() {
        List<MenuDish> rows = List.of(row(SHRIMP_SOUP));

        assertThat(PortionContent.of(input(rows, Texture.REGULAR, null, SHRIMP_AND_PEANUT, false))
                        .flags())
                .containsExactly(PortionFlag.MISSING_SUBSTITUTE_DISH);
        Portion safe = PortionContent.of(input(rows, Texture.REGULAR, null, SHRIMP_AND_PEANUT, true));
        assertThat(safe.flags()).containsExactly(PortionFlag.SAFE_DISH);
        assertThat(safe.dishes()).containsExactly(new DishChoice(SHRIMP_SOUP, SAFE));
        Portion conflicting = PortionContent.of(input(rows, Texture.REGULAR, null, id -> true, true));
        assertThat(conflicting.flags()).containsExactly(PortionFlag.MISSING_SUBSTITUTE_DISH);
        assertThat(conflicting.safeDishConflicts()).isTrue();
    }

    @Test
    void FR_038_noDishForDiet_isMarked_safeDishFillsIt() {
        assertThat(PortionContent.of(input(List.of(), Texture.REGULAR, null, NONE, false))
                        .flags())
                .containsExactly(PortionFlag.NO_DISH_IN_MENU);
        Portion safe = PortionContent.of(input(List.of(), Texture.REGULAR, null, NONE, true));
        assertThat(safe.flags()).containsExactly(PortionFlag.SAFE_DISH);
        assertThat(safe.dishes()).containsExactly(new DishChoice(SAFE, null));
        assertThat(PortionContent.of(new Input(List.of(), false, Texture.REGULAR, null, NONE, SAFE, false, false))
                        .flags())
                .isEmpty();
    }

    @Test
    void FR_036a_otherAllergy_isCrossCheckFlag_withoutMakingPortionSpecial() {
        Portion p = PortionContent.of(
                new Input(List.of(row(PLAIN_SOUP)), true, Texture.REGULAR, null, NONE, SAFE, false, true));

        assertThat(p.flags()).isEqualTo(Set.of(PortionFlag.NEEDS_CROSS_CHECK));
        assertThat(p.special()).isFalse();
    }

    @Test
    void FR_036_familyPortion_mainDishesOnly() {
        assertThat(PortionContent.family(List.of(row(SHRIMP_SOUP, PLAIN_SOUP))))
                .containsExactly(new DishChoice(SHRIMP_SOUP, null));
    }
}
