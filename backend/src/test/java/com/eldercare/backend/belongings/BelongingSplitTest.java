package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.belongings.internal.belonging.domain.BelongingSplit;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 013 T004 (research K-04; FR-012, FR-013, SC-001): tách đồ khi bàn giao một phần, tách phần đếm thiếu. */
class BelongingSplitTest {

    private static BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    @Test
    void wholeItem_noSplit_originalMoves() {
        var plan = BelongingSplit.plan(n("10"), null, null);

        assertThat(plan.originalMoves()).isTrue();
        assertThat(plan.remaining()).isZero();
        assertThat(plan.moved()).isEqualByComparingTo("10");
        assertThat(plan.missing()).isZero();
        assertThat(plan.needsSplit()).isFalse();
    }

    @Test
    void FR_012_partialHandover_splitsMovedPart_originalKeepsRemainder() {
        var plan = BelongingSplit.plan(n("10"), n("4"), null);

        assertThat(plan.originalMoves()).isFalse();
        assertThat(plan.remaining()).isEqualByComparingTo("6");
        assertThat(plan.moved()).isEqualByComparingTo("4");
        assertThat(plan.missing()).isZero();
        assertThat(plan.needsSplit()).isTrue();
    }

    @Test
    void FR_013_countedLess_splitsMissingPartAsLost() {
        var plan = BelongingSplit.plan(n("6"), n("6"), n("5"));

        assertThat(plan.originalMoves()).isTrue();
        assertThat(plan.moved()).isEqualByComparingTo("5");
        assertThat(plan.missing()).isEqualByComparingTo("1");
        assertThat(plan.needsSplit()).isTrue();
    }

    @Test
    void SC_001_partsAlwaysSumToCurrentQuantity() {
        for (String[] c : new String[][] {{"10", "4", "3"}, {"2.5", "1.5", null}, {"2000000", "2000000", "1500000"}}) {
            var current = n(c[0]);
            var plan = BelongingSplit.plan(current, n(c[1]), c[2] == null ? null : n(c[2]));
            assertThat(plan.remaining().add(plan.moved()).add(plan.missing())).isEqualByComparingTo(current);
        }
    }

    @Test
    void invalidQuantities_areRejected() {
        assertThatThrownBy(() -> BelongingSplit.plan(n("10"), n("11"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BelongingSplit.plan(n("10"), n("0"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BelongingSplit.plan(n("10"), n("4"), n("5")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BelongingSplit.plan(n("10"), n("4"), n("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countedZero_meansWholeHandedPartIsMissing() {
        var plan = BelongingSplit.plan(n("3"), null, n("0"));

        assertThat(plan.moved()).isZero();
        assertThat(plan.missing()).isEqualByComparingTo("3");
    }
}
