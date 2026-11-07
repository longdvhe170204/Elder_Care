package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.health.api.dto.MeasurementFrequency;
import com.eldercare.backend.health.internal.schedule.domain.MeasurementTimeline;
import com.eldercare.backend.health.internal.schedule.domain.ScheduleFrequency;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Ba kiểu tần suất của lịch đo (FR-003; research S-05 {@code ChuoiMocDo}); giờ theo múi giờ của viện (UTC+7). */
class MeasurementTimelineTest {
    // 2026-11-04 là thứ Tư; 00:00 giờ viện = 2026-11-03T17:00Z.
    private static final Instant START = Instant.parse("2026-11-03T17:00:00Z");

    private static ScheduleFrequency clock(String... times) {
        return new ScheduleFrequency(
                MeasurementFrequency.CLOCK_TIMES,
                List.of(times).stream().map(LocalTime::parse).toList(),
                null,
                null,
                Set.of());
    }

    @Test
    void FR_003_clockTimesEveryDayWithinScheduleAndWindow() {
        var occurrences = MeasurementTimeline.occurrences(
                clock("06:00", "18:00"), START, START.plusSeconds(2 * 86400), START, START.plusSeconds(3 * 86400));

        assertThat(occurrences)
                .containsExactly(
                        Instant.parse("2026-11-03T23:00:00Z"),
                        Instant.parse("2026-11-04T11:00:00Z"),
                        Instant.parse("2026-11-04T23:00:00Z"),
                        Instant.parse("2026-11-05T11:00:00Z"));
    }

    @Test
    void FR_003_everyNHoursFromAnchor() {
        var frequency = new ScheduleFrequency(
                MeasurementFrequency.EVERY_N_HOURS, List.of(), 4, Instant.parse("2026-11-04T01:00:00Z"), Set.of());

        var occurrences = MeasurementTimeline.occurrences(
                frequency, START, null, Instant.parse("2026-11-04T02:00:00Z"), Instant.parse("2026-11-04T14:00:00Z"));

        assertThat(occurrences)
                .containsExactly(
                        Instant.parse("2026-11-04T05:00:00Z"),
                        Instant.parse("2026-11-04T09:00:00Z"),
                        Instant.parse("2026-11-04T13:00:00Z"));
    }
}
