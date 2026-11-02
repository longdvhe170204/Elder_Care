package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class BR_M04_09_PoorMealStreakTest {
    @Test
    void BR_M04_09_countsLatestConsecutivePoorMealsAndStopsAtGoodMeal() throws Exception {
        var count = Class.forName("com.eldercare.backend.care.internal.rules.domain.PoorMealStreak")
                .getMethod("count", List.class);
        assertThat(count.invoke(null, List.of("SOME", "NOT_EATEN", "SKIPPED", "MOST", "SOME")))
                .isEqualTo(3);
        assertThat(count.invoke(null, List.of("ATE_ALL", "SOME"))).isEqualTo(0);
        assertThat(count.invoke(null, List.of())).isEqualTo(0);
    }
}
