package com.eldercare.backend.family.internal.stay.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * 012 T034 ({@code SoDemTest}; FR-043, BR-M10-04; quickstart Q31, Q32): số đêm là số mốc 00:00 nằm sau lúc bắt đầu và
 * không sau lúc kết thúc; mỗi đêm mang ngày bắt đầu của đêm.
 */
class NightCountTest {

    private static Instant at(String local) {
        return LocalDateTime.parse(local).atZone(BusinessTime.ZONE).toInstant();
    }

    @Test
    void Q31_evening01To08Of03_isTwoNights() {
        Instant start = at("2026-11-01T18:00");
        Instant end = at("2026-11-03T08:00");

        assertThat(NightCount.count(start, end)).isEqualTo(2);
        assertThat(NightCount.nights(start, end)).containsExactly(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));
    }

    @Test
    void US8_sameDay08To20_isZeroNights() {
        assertThat(NightCount.count(at("2026-11-01T08:00"), at("2026-11-01T20:00")))
                .isZero();
    }

    @Test
    void Q32_endBeforeSecondMidnight_isOneNight() {
        assertThat(NightCount.nights(at("2026-11-01T18:00"), at("2026-11-02T23:00")))
                .containsExactly(LocalDate.of(2026, 11, 1));
    }

    @Test
    void FR_043_startingAtMidnight_doesNotCrossIt_endingAtMidnight_does() {
        assertThat(NightCount.count(at("2026-11-02T00:00"), at("2026-11-02T10:00")))
                .isZero();
        assertThat(NightCount.nights(at("2026-11-01T20:00"), at("2026-11-02T00:00")))
                .containsExactly(LocalDate.of(2026, 11, 1));
    }

    @Test
    void FR_043_endNotAfterStart_isZero() {
        Instant t = at("2026-11-01T18:00");
        assertThat(NightCount.count(t, t)).isZero();
    }
}
