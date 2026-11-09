package com.eldercare.backend.staffing.internal.ratio.service;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.resident.api.dto.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class StaffingPresenceTest {
    final Instant start = Instant.parse("2026-10-25T00:00:00Z"),
            end = start.plusSeconds(28800),
            middle = start.plusSeconds(14400);

    private StatusPeriod period(ResidentStatus status, Instant from, Instant to) {
        return new StatusPeriod(status, from, Optional.ofNullable(to));
    }

    @Test
    void FR_034_adjacentAbsenceStatusesCoverWholeShift_inAnyOrder() {
        assertThat(StaffingRatioSources.fullyAbsent(
                        List.of(
                                period(ResidentStatus.HOSPITALIZED, middle, end),
                                period(ResidentStatus.TEMPORARILY_ABSENT, start, middle)),
                        start,
                        end))
                .isTrue();
    }

    @Test
    void FR_034_gapOrAbsenceEndingAtStartDoesNotCoverShift() {
        assertThat(StaffingRatioSources.fullyAbsent(
                        List.of(
                                period(ResidentStatus.HOSPITALIZED, middle.plusNanos(1), end),
                                period(ResidentStatus.TEMPORARILY_ABSENT, start, middle)),
                        start,
                        end))
                .isFalse();
        assertThat(StaffingRatioSources.fullyAbsent(
                        List.of(period(ResidentStatus.TEMPORARILY_ABSENT, start.minusSeconds(1), start)), start, end))
                .isFalse();
    }
}
