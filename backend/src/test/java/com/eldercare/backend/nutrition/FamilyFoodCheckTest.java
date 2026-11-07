package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.api.dto.FamilyFoodStatus;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFoodCheck;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFoodCheck.Food;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFoodCheck.Person;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFoodCheck.Result;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 011 T034 (US7, FR-053, FR-054, BR-M08-04; quickstart Q42): hàm thuần đối chiếu đồ ăn gia đình với người nhận: Không sử
 * dụng khi loại bị cấm, hết hạn, thành phần xung đột; Cần xác nhận khi có điểm nghi vấn; ngược lại Được sử dụng.
 */
class FamilyFoodCheckTest {

    private static final Instant NOW = Instant.parse("2026-11-10T03:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(3600);
    private static final long PEANUT = 11;
    private static final long MILK = 12;

    private static Food food(String type, Set<Long> ingredients, boolean unknown, boolean otherGroup, boolean related) {
        return new Food(type, ingredients, unknown, otherGroup, LATER, related);
    }

    private static Food fruit() {
        return food("Trái cây", Set.of(), false, false, true);
    }

    private static Person healthy() {
        return new Person(List.of(), Set.of(), Set.of(), Texture.REGULAR, false);
    }

    private static Result check(Food food, Person person) {
        return FamilyFoodCheck.check(food, person, Set.of("Đồ uống có cồn"), NOW);
    }

    @Test
    void FR_053_noIssue_isAllowed() {
        Result result = check(fruit(), healthy());

        assertThat(result.status()).isEqualTo(FamilyFoodStatus.ALLOWED);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void BR_M08_04_ingredientMatchingAllergy_isNotAllowedWithAllergyReason() {
        Person allergic =
                new Person(List.of(new AllergyItem(PEANUT, false)), Set.of(), Set.of(), Texture.REGULAR, false);

        Result result = check(food("Bánh quy", Set.of(PEANUT), false, false, true), allergic);

        assertThat(result.status()).isEqualTo(FamilyFoodStatus.NOT_ALLOWED);
        assertThat(result.reasons()).containsExactly(FamilyFoodCheck.ALLERGY);
    }

    @Test
    void BR_M08_04_personalAndDietRestrictions_areNotAllowedReasons() {
        Person restricted = new Person(List.of(), Set.of(MILK), Set.of(PEANUT), Texture.REGULAR, false);

        Result result = check(food("Bánh", Set.of(MILK, PEANUT), false, false, true), restricted);

        assertThat(result.status()).isEqualTo(FamilyFoodStatus.NOT_ALLOWED);
        assertThat(result.reasons())
                .containsExactlyInAnyOrder(FamilyFoodCheck.PERSONAL_RESTRICTION, FamilyFoodCheck.DIET_RESTRICTION);
    }

    @Test
    void FR_053_forbiddenType_isNotAllowed_matchedIgnoringCaseAndSpaces() {
        Result result = check(food("  đồ uống CÓ cồn ", Set.of(), false, false, true), healthy());

        assertThat(result.status()).isEqualTo(FamilyFoodStatus.NOT_ALLOWED);
        assertThat(result.reasons()).containsExactly(FamilyFoodCheck.FORBIDDEN_TYPE);
    }

    @Test
    void FR_054_unknownIngredients_needsConfirmation() {
        Result result = check(food("Hộp cơm", Set.of(), true, false, true), healthy());

        assertThat(result.status()).isEqualTo(FamilyFoodStatus.NEEDS_CONFIRMATION);
        assertThat(result.reasons()).containsExactly(FamilyFoodCheck.INGREDIENTS_UNKNOWN);
    }

    @Test
    void FR_054_otherGroupIngredient_needsConfirmation() {
        Result result = check(food("Bánh", Set.of(PEANUT), false, true, true), healthy());

        assertThat(result.status()).isEqualTo(FamilyFoodStatus.NEEDS_CONFIRMATION);
        assertThat(result.reasons()).containsExactly(FamilyFoodCheck.OTHER_INGREDIENT);
    }
}
