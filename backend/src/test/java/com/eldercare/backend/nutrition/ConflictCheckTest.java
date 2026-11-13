package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 011 T005, research M-06: hàm thuần đối chiếu xung đột, test bảng theo BR-M08-02, BR-M01-07. */
class ConflictCheckTest {

    private static final long FISH = 1;
    private static final long PEANUT = 2;
    private static final long SUGAR = 3;
    private static final long SALT = 4;

    @Test
    void BR_M08_02_ingredientMatchesActiveAllergen_isAllergyConflict() {
        assertThat(ConflictCheck.conflicts(
                        Set.of(FISH, SALT), List.of(new AllergyItem(FISH, false)), Set.of(), Set.of()))
                .containsExactly(ConflictType.ALLERGY);
    }

    @Test
    void BR_M01_07_otherAllergyNotAutoChecked_isNeverMatched() {
        assertThat(ConflictCheck.conflicts(
                        Set.of(FISH),
                        List.of(new AllergyItem(FISH, true), new AllergyItem(null, true)),
                        Set.of(),
                        Set.of()))
                .isEmpty();
    }

    @Test
    void BR_M01_07_allergyWithoutAllergenId_isIgnored() {
        assertThat(ConflictCheck.conflicts(Set.of(FISH), List.of(new AllergyItem(null, false)), Set.of(), Set.of()))
                .isEmpty();
    }
}
