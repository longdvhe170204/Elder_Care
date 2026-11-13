package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.service.ClosedDoseView;
import com.eldercare.backend.medication.internal.dose.service.ConsecutiveRefusalCounter;
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
class BR_27_ConsecutiveRefusalTest {

    // Ngưỡng của test, không phải mặc định CFG-M05-04 (service đọc qua ParameterApi)
    private static final int THRESHOLD = 3;

    @Test
    @DisplayName("Empty list or no refusals returns 0")
    void testEmptyOrNoRefusals() {
        assertThat(ConsecutiveRefusalCounter.count(Collections.emptyList())).isEqualTo(0);

        Instant now = Instant.now();
        List<ClosedDoseView> doses = List.of(
                new ClosedDoseView(1L, now, DoseStatus.ADMINISTERED),
                new ClosedDoseView(2L, now.minus(4, ChronoUnit.HOURS), DoseStatus.NOT_ADMINISTERED));

        assertThat(ConsecutiveRefusalCounter.count(doses)).isEqualTo(0);
        assertThat(ConsecutiveRefusalCounter.meetsThreshold(0, THRESHOLD)).isFalse();
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
        assertThat(ConsecutiveRefusalCounter.meetsThreshold(count, THRESHOLD)).isTrue();
    }
}
