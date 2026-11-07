package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.alert.domain.RepeatWithin24HoursCounter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Đếm phát sinh mức Trung bình trong 24 giờ trượt (FR-038a, research S-08). */
class RepeatWithin24HoursCounterTest {
    private static final Instant T = Instant.parse("2030-03-04T12:00:00Z");
    private static final Duration DAY = Duration.ofHours(24);

    @Test
    void BR_M05_03_countsOnlyWindowOpenAtStartClosedAtEnd() {
        var times = List.of(
                T.minusSeconds(24 * 3600), // đúng mốc t − 24h: không tính
                T.minusSeconds(24 * 3600 - 1),
                T.minusSeconds(3600),
                T); // đúng t: tính
        assertThat(RepeatWithin24HoursCounter.count(times, T, DAY)).isEqualTo(3);
    }

    @Test
    void BR_M05_03_ignoresLaterOccurrences() {
        assertThat(RepeatWithin24HoursCounter.count(List.of(T.plusSeconds(1)), T, DAY))
                .isZero();
    }
}
