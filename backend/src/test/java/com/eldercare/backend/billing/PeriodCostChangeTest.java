package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.period.domain.PeriodCostChange;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PeriodCostChangeTest {

    @Test
    void BR_M11_07_warningContainsTheRatioAndLargestPositiveIncreases() {
        var warning = PeriodCostChange.compare(
                        Map.of(
                                CostType.RESIDENCY_FEE,
                                9_000_000L,
                                CostType.SERVICE,
                                3_000_000L,
                                CostType.CONSUMABLE,
                                1_200_000L),
                        Map.of(CostType.RESIDENCY_FEE, 9_000_000L, CostType.SERVICE, 800_000L),
                        true,
                        new BigDecimal("30"))
                .orElseThrow();
        assertThat(warning.increaseRatio()).isBetween(new BigDecimal("34.69"), new BigDecimal("34.70"));
        assertThat(warning.topThreeIncreases()).containsExactly("SERVICE", "CONSUMABLE");
    }

    @Test
    void BR_M11_07_thresholdIsStrictAndFullMonthsAreRequired() {
        var previous = Map.of(CostType.SERVICE, 100L);
        assertThat(PeriodCostChange.compare(Map.of(CostType.SERVICE, 130L), previous, true, new BigDecimal("30")))
                .isEmpty();
        assertThat(PeriodCostChange.compare(Map.of(CostType.SERVICE, 200L), previous, false, new BigDecimal("30")))
                .isEmpty();
        assertThat(PeriodCostChange.compare(Map.of(CostType.SERVICE, 80L), previous, true, BigDecimal.ZERO))
                .isEmpty();
    }

    @Test
    void BR_M11_07_rankingIsStableAndLimitedToThreeTypes() {
        var current = Map.of(
                CostType.SERVICE, 40L, CostType.MEDICATION, 30L, CostType.CONSUMABLE, 20L, CostType.ACTIVITY, 10L);
        var warning = PeriodCostChange.compare(current, Map.of(CostType.SERVICE, 1L), true, BigDecimal.ZERO)
                .orElseThrow();
        assertThat(warning.topThreeIncreases()).containsExactly("SERVICE", "MEDICATION", "CONSUMABLE");
    }
}
