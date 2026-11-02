package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.staffing.handover.internal.domain.NextHandoverShift;
import com.eldercare.backend.staffing.shift.internal.domain.WorkShift;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class NextHandoverShiftTest {
    private final Instant origin = Instant.parse("2026-10-24T00:00:00Z");

    private WorkShift shift(
            long id, long start, long end, String scope, long scopeId, boolean required, String status) {
        return new WorkShift(
                id,
                1,
                1,
                origin.plusSeconds(start * 3600),
                origin.plusSeconds(end * 3600),
                scope,
                scopeId,
                required,
                status,
                1L,
                false,
                0);
    }

    @Test
    void FR_038_earliestOverlappingOrAdjacentShiftWinsInExactScope() {
        var previous = shift(1, 0, 8, "FLOOR", 2, true, "IN_PROGRESS");
        var later = shift(2, 8, 16, "FLOOR", 2, true, "PUBLISHED");
        var overlap = shift(3, 7, 15, "FLOOR", 2, true, "IN_PROGRESS");
        var others = List.of(
                later,
                shift(4, 1, 9, "AREA", 2, true, "PUBLISHED"),
                previous,
                shift(5, 2, 10, "FLOOR", 3, true, "PUBLISHED"),
                overlap);
        assertThat(NextHandoverShift.find(previous, others, Duration.ofHours(24)))
                .contains(overlap);
    }

    @Test
    void FR_038_fallbackSearchIncludesConfiguredBoundaryAndExcludesInvalidCandidates() {
        var previous = shift(1, 0, 8, "FLOOR", 2, true, "CLOSED");
        var boundary = shift(2, 32, 40, "FLOOR", 2, true, "PUBLISHED");
        assertThat(NextHandoverShift.find(
                        previous,
                        List.of(
                                shift(3, 8, 16, "FLOOR", 2, false, "PUBLISHED"),
                                shift(4, 9, 17, "FLOOR", 2, true, "DRAFT"),
                                shift(5, 10, 18, "FLOOR", 2, true, "CANCELLED"),
                                boundary),
                        Duration.ofHours(24)))
                .contains(boundary);
        assertThat(NextHandoverShift.find(previous, List.of(boundary), Duration.ofHours(23)))
                .isEmpty();
    }
}
