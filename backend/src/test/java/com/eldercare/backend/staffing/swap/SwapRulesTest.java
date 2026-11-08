package com.eldercare.backend.staffing.swap;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.internal.shift.domain.ContinuousShiftChain.Interval;
import com.eldercare.backend.staffing.internal.swap.domain.SwapRules;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FR-021 (a), (g): hàm thuần kiểm tra chồng giờ và đếm giờ trong tháng. */
class SwapRulesTest {

    private static Instant at(int day, int hour) {
        return LocalDateTime.of(2026, 11, day, hour, 0).atZone(BusinessTime.ZONE).toInstant();
    }

    private static Interval shift(int day, int from, int to) {
        return new Interval(at(day, from), at(day, to));
    }

    @Test
    void BR_M09_03_overlappingShiftsAreDetected() {
        assertThat(SwapRules.overlaps(shift(10, 6, 14), shift(10, 13, 21))).isTrue();
        assertThat(SwapRules.overlaps(shift(10, 6, 14), shift(10, 8, 10))).isTrue();
    }

    @Test
    void BR_M09_03_touchingShiftsDoNotOverlap() {
        assertThat(SwapRules.overlaps(shift(10, 6, 14), shift(10, 14, 22))).isFalse();
        assertThat(SwapRules.anyOverlap(List.of(shift(10, 6, 14), shift(11, 6, 14)), shift(12, 6, 14)))
                .isFalse();
    }

    @Test
    void FR_021_g_hoursInMonthClipShiftsCrossingMonthBoundary() {
        Interval crossing = new Interval(
                LocalDateTime.of(2026, 11, 30, 22, 0).atZone(BusinessTime.ZONE).toInstant(),
                LocalDateTime.of(2026, 12, 1, 6, 0).atZone(BusinessTime.ZONE).toInstant());
        assertThat(SwapRules.hoursInMonth(List.of(shift(2, 6, 14), crossing), YearMonth.of(2026, 11)))
                .isEqualTo(Duration.ofHours(10));
        assertThat(SwapRules.hoursInMonth(List.of(crossing), YearMonth.of(2026, 12)))
                .isEqualTo(Duration.ofHours(6));
    }
}
