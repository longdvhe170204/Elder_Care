package com.cambae.backend.m09.ratio.internal.service;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.m01.api.dto.*;
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

    @Test
    void FR_034_openAbsenceFromBeforeShiftCovers_it_emptyHistoryDoesNot() {
        assertThat(StaffingRatioSources.fullyAbsent(
                        List.of(period(ResidentStatus.OFF_SITE_ACTIVITY, start.minusSeconds(1), null)), start, end))
                .isTrue();
        assertThat(StaffingRatioSources.fullyAbsent(List.of(), start, end)).isFalse();
    }
}
