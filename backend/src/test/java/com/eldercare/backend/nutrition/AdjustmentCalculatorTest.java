package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.closing.domain.AdjustmentCalculator;
import com.eldercare.backend.nutrition.internal.closing.domain.AdjustmentCalculator.Change;
import com.eldercare.backend.nutrition.internal.closing.domain.AdjustmentCalculator.Delta;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedKind;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson.DishChoice;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 011 T021 (FR-039, BR-M08-13; tên {@code TinhPhatSinhTest} trong tasks): người mới "+1", người không còn được tính "−1",
 * đổi tầng hoặc chế độ ăn "−1" chỗ cũ và "+1" chỗ mới, đổi nội dung suất đặc biệt là phát sinh 0 suất; không đổi thì rỗng.
 */
class AdjustmentCalculatorTest {

    private static CountedPerson person(long resident, long floor, long diet, Texture texture) {
        return new CountedPerson(
                CountedKind.RESIDENTIAL,
                resident,
                null,
                null,
                floor,
                diet,
                null,
                texture,
                List.of(new DishChoice(1, null)),
                Set.of(),
                texture != Texture.REGULAR);
    }

    private static Map<String, CountedPerson> map(CountedPerson... people) {
        java.util.LinkedHashMap<String, CountedPerson> out = new java.util.LinkedHashMap<>();
        for (CountedPerson p : people) {
            out.put(p.key(), p);
        }
        return out;
    }

    @Test
    void FR_039_noChange_noDelta() {
        CountedPerson a = person(1, 2, 5, Texture.REGULAR);

        assertThat(AdjustmentCalculator.diff(map(a), map(a))).isEmpty();
    }

    @Test
    void FR_039_addedAndRemoved_arePlusAndMinusOne() {
        CountedPerson a = person(1, 2, 5, Texture.REGULAR);
        CountedPerson b = person(2, 2, 5, Texture.REGULAR);

        assertThat(AdjustmentCalculator.diff(map(a), map(b)))
                .extracting(Delta::floorAreaId, Delta::dietId, Delta::portionCount, Delta::change)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, 5L, -1, Change.REMOVED),
                        org.assertj.core.groups.Tuple.tuple(2L, 5L, 1, Change.ADDED));
    }

    @Test
    void FR_039_bedTransfer_isMinusOldFloorPlusNewFloor() {
        CountedPerson before = person(1, 2, 5, Texture.PUREED);
        CountedPerson after = person(1, 3, 5, Texture.PUREED);

        assertThat(AdjustmentCalculator.diff(map(before), map(after)))
                .extracting(Delta::floorAreaId, Delta::portionCount, Delta::change)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, -1, Change.MOVED_OUT),
                        org.assertj.core.groups.Tuple.tuple(3L, 1, Change.MOVED_IN));
    }

    @Test
    void FR_039_menuDishChange_ofPlainPortion_isNotPerPersonDelta() {
        CountedPerson before = person(1, 2, 5, Texture.REGULAR);
        CountedPerson after = new CountedPerson(
                CountedKind.RESIDENTIAL,
                1,
                null,
                null,
                2,
                5,
                null,
                Texture.REGULAR,
                List.of(new DishChoice(2, null)),
                Set.of(),
                false);

        assertThat(AdjustmentCalculator.diff(map(before), map(after))).isEmpty();
    }

    @Test
    void FR_039_specialContentChange_isZeroPortionDelta() {
        CountedPerson before = person(1, 2, 5, Texture.REGULAR);
        CountedPerson after = person(1, 2, 5, Texture.SOFT);

        assertThat(AdjustmentCalculator.diff(map(before), map(after)))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.portionCount()).isZero();
                    assertThat(d.change()).isEqualTo(Change.PORTION_CHANGED);
                    assertThat(d.after().texture()).isEqualTo(Texture.SOFT);
                });
    }
}
