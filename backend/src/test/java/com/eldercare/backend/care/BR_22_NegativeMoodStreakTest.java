package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BR_22_NegativeMoodStreakTest {
    @Test
    void BR_22_requiresConsecutiveCalendarDaysAndCountsADayOnlyOnce() throws Exception {
        var d = LocalDate.of(2026, 11, 2);
        var count = Class.forName("com.eldercare.backend.care.internal.rules.domain.NegativeMoodStreak")
                .getMethod("count", Set.class, LocalDate.class);
        assertThat(count.invoke(null, Set.of(d, d.minusDays(1), d.minusDays(2)), d))
                .isEqualTo(3);
        assertThat(count.invoke(null, Set.of(d, d.minusDays(2)), d)).isEqualTo(1);
        assertThat(count.invoke(null, Set.of(d.minusDays(1)), d)).isEqualTo(0);
    }
}
