package com.cambae.backend.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** BR-M15-07: ngày hiệu lực là ngày lịch theo Asia/Ho_Chi_Minh, có hiệu lực từ 00:00 của ngày đó (000 T014). */
class EffectiveDateTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    // 2026-10-19 23:30 UTC là 2026-10-20 06:30 giờ Việt Nam
    private final BusinessTime time = new BusinessTime(Clock.fixed(Instant.parse("2026-10-19T23:30:00Z"), VN));

    @Test
    void BR_M15_07_startOfDay_isMidnightVietnamTime() {
        assertThat(time.startOfDay(LocalDate.of(2026, 10, 20))).isEqualTo(Instant.parse("2026-10-19T17:00:00Z"));
    }

    @Test
    void BR_M15_07_today_followsVietnamCalendarNotUtc() {
        assertThat(time.today()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void FR_046_now_comesFromInjectedClock() {
        assertThat(time.now()).isEqualTo(Instant.parse("2026-10-19T23:30:00Z"));
    }

    @Test
    void BR_M15_07_isWithin_includesBothEnds() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        assertThat(time.isWithin(from, from, to)).isTrue();
        assertThat(time.isWithin(to, from, to)).isTrue();
        assertThat(time.isWithin(LocalDate.of(2026, 10, 15), from, to)).isTrue();
        assertThat(time.isWithin(LocalDate.of(2026, 9, 30), from, to)).isFalse();
        assertThat(time.isWithin(LocalDate.of(2026, 11, 1), from, to)).isFalse();
    }

    @Test
    void BR_M15_07_isWithin_nullEndMeansOpenEnded() {
        LocalDate from = LocalDate.of(2026, 10, 1);

        assertThat(time.isWithin(LocalDate.of(2099, 1, 1), from, null)).isTrue();
        assertThat(time.isWithin(LocalDate.of(2026, 9, 30), from, null)).isFalse();
    }
}
