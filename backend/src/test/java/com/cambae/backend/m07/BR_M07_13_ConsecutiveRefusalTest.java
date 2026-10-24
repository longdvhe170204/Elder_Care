package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m07.internal.domain.DoseStatus;
import com.cambae.backend.m07.internal.service.ClosedDoseView;
import com.cambae.backend.m07.internal.service.ConsecutiveRefusalCounter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T018: Unit test for Consecutive Refusal calculation (FR-032, BR-M07-13).
 * Verifies that >= CFG-M05-04 (default 3) consecutive refusals are counted in reverse chronological order,
 * ignoring suspended/cancelled doses, and broken by administration or other non-refusal statuses.
 */
class BR_M07_13_ConsecutiveRefusalTest {

    @Test
    @DisplayName("Empty list or no refusals returns 0")
    void testEmptyOrNoRefusals() {
        assertThat(ConsecutiveRefusalCounter.count(Collections.emptyList())).isEqualTo(0);

        Instant now = Instant.now();
        List<ClosedDoseView> doses = List.of(
                new ClosedDoseView(1L, now, DoseStatus.ADMINISTERED),
                new ClosedDoseView(2L, now.minus(4, ChronoUnit.HOURS), DoseStatus.NOT_ADMINISTERED));

        assertThat(ConsecutiveRefusalCounter.count(doses)).isEqualTo(0);
        assertThat(ConsecutiveRefusalCounter.meetsThreshold(0, ConsecutiveRefusalCounter.DEFAULT_REFUSAL_THRESHOLD))
                .isFalse();
    }

    @Test
    @DisplayName("Three consecutive refusals meets alert threshold")
    void testThreeConsecutiveRefusals() {
        Instant now = Instant.now();
        List<ClosedDoseView> doses = List.of(
                new ClosedDoseView(3L, now, DoseStatus.REFUSED),
                new ClosedDoseView(2L, now.minus(4, ChronoUnit.HOURS), DoseStatus.REFUSED),
                new ClosedDoseView(1L, now.minus(8, ChronoUnit.HOURS), DoseStatus.REFUSED),
                new ClosedDoseView(0L, now.minus(12, ChronoUnit.HOURS), DoseStatus.ADMINISTERED));

        int count = ConsecutiveRefusalCounter.count(doses);
        assertThat(count).isEqualTo(3);
        assertThat(ConsecutiveRefusalCounter.meetsThreshold(count, ConsecutiveRefusalCounter.DEFAULT_REFUSAL_THRESHOLD))
                .isTrue();
    }

    @Test
    @DisplayName("Streak is broken by ADMINISTERED or MISSED dose")
    void testStreakBroken() {
        Instant now = Instant.now();
        // 2 refusals, then an ADMINISTERED dose, then 2 older refusals
        List<ClosedDoseView> doses = List.of(
                new ClosedDoseView(5L, now, DoseStatus.REFUSED),
                new ClosedDoseView(4L, now.minus(4, ChronoUnit.HOURS), DoseStatus.REFUSED),
                new ClosedDoseView(3L, now.minus(8, ChronoUnit.HOURS), DoseStatus.ADMINISTERED),
                new ClosedDoseView(2L, now.minus(12, ChronoUnit.HOURS), DoseStatus.REFUSED),
                new ClosedDoseView(1L, now.minus(16, ChronoUnit.HOURS), DoseStatus.REFUSED));

        int count = ConsecutiveRefusalCounter.count(doses);
        assertThat(count).isEqualTo(2);
        assertThat(ConsecutiveRefusalCounter.meetsThreshold(count, ConsecutiveRefusalCounter.DEFAULT_REFUSAL_THRESHOLD))
                .isFalse();
    }

    @Test
    @DisplayName("SUSPENDED and CANCELLED doses are skipped without breaking streak")
    void testSuspendedAndCancelledSkipped() {
        Instant now = Instant.now();
        // refusal, cancelled, refusal, suspended, refusal -> count = 3
        List<ClosedDoseView> doses = List.of(
                new ClosedDoseView(5L, now, DoseStatus.REFUSED),
                new ClosedDoseView(4L, now.minus(2, ChronoUnit.HOURS), DoseStatus.CANCELLED),
                new ClosedDoseView(3L, now.minus(4, ChronoUnit.HOURS), DoseStatus.REFUSED),
                new ClosedDoseView(2L, now.minus(6, ChronoUnit.HOURS), DoseStatus.SUSPENDED),
                new ClosedDoseView(1L, now.minus(8, ChronoUnit.HOURS), DoseStatus.REFUSED),
                new ClosedDoseView(0L, now.minus(12, ChronoUnit.HOURS), DoseStatus.ADMINISTERED));

        int count = ConsecutiveRefusalCounter.count(doses);
        assertThat(count).isEqualTo(3);
        assertThat(ConsecutiveRefusalCounter.meetsThreshold(count, ConsecutiveRefusalCounter.DEFAULT_REFUSAL_THRESHOLD))
                .isTrue();
    }
}
