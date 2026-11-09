package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.finalperiod.domain.FinalPeriodCondition;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FinalPeriodConditionTest {
    private final LocalDate day = LocalDate.of(2026, 11, 20);

    @Test
    void FR_034a_requiresFinalizedStatementAndEveryRequiredDay() {
        assertThat(FinalPeriodCondition.reasons(false, Set.of(day), Set.of(day), false))
                .isNotEmpty();
        assertThat(FinalPeriodCondition.reasons(true, Set.of(day), Set.of(), false))
                .isNotEmpty();
        assertThat(FinalPeriodCondition.reasons(true, Set.of(day), Set.of(day), false))
                .isEmpty();
    }

    @Test
    void FR_035_overdueDayMustAlsoHaveFinalizedFee() {
        assertThat(FinalPeriodCondition.reasons(true, Set.of(day, day.plusDays(1)), Set.of(day), false))
                .isNotEmpty();
    }

    @Test
    void FR_029_onlyEndingAffectingAdjustmentsBlock() {
        assertThat(FinalPeriodCondition.reasons(true, Set.of(day), Set.of(day), true))
                .isNotEmpty();
        assertThat(FinalPeriodCondition.reasons(true, Set.of(day), Set.of(day), false))
                .isEmpty();
    }
}
