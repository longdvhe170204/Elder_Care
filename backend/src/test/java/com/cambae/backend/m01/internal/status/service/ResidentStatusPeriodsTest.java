package com.cambae.backend.m01.internal.status.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cambae.backend.m01.api.dto.*;
import com.cambae.backend.m01.internal.status.domain.ResidentStatusHistory;
import com.cambae.backend.m01.internal.status.persistence.ResidentStatusHistoryRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ResidentStatusPeriodsTest {
    @Test
    void FR_039_periodsRetainActualBoundariesAndRepeatedReturnsWithHalfOpenOverlap() {
        var history = mock(ResidentStatusHistoryRepository.class);
        var api = new ResidentStatusApiImpl(mock(ResidentStatusService.class), history);
        var t = Instant.parse("2035-04-01T00:00:00Z");
        when(history.findByResidentIdOrderByIdAsc(7))
                .thenReturn(List.of(
                        row(ResidentStatus.IN_RESIDENCE, t), row(ResidentStatus.TEMPORARILY_ABSENT, t.plusSeconds(10)),
                        row(ResidentStatus.IN_RESIDENCE, t.plusSeconds(20)),
                                row(ResidentStatus.HOSPITALIZED, t.plusSeconds(30))));
        assertThat(api.statusPeriods(7, Set.of(ResidentStatus.IN_RESIDENCE), t.plusSeconds(5), t.plusSeconds(30)))
                .containsExactly(
                        new StatusPeriod(ResidentStatus.IN_RESIDENCE, t, Optional.of(t.plusSeconds(10))),
                        new StatusPeriod(
                                ResidentStatus.IN_RESIDENCE, t.plusSeconds(20), Optional.of(t.plusSeconds(30))));
        assertThat(api.statusPeriods(7, Set.of(ResidentStatus.HOSPITALIZED), t.plusSeconds(30), t.plusSeconds(40)))
                .containsExactly(new StatusPeriod(ResidentStatus.HOSPITALIZED, t.plusSeconds(30), Optional.empty()));
        assertThat(api.statusPeriods(
                        7, Set.of(ResidentStatus.TEMPORARILY_ABSENT), t.plusSeconds(20), t.plusSeconds(30)))
                .isEmpty();
    }

    @Test
    void FR_039_emptySelectionOrEmptyRangeDoesNotReadHistory() {
        var api = new ResidentStatusApiImpl(
                mock(ResidentStatusService.class), mock(ResidentStatusHistoryRepository.class));
        assertThat(api.statusPeriods(7, Set.of(), Instant.EPOCH, Instant.EPOCH.plusSeconds(1)))
                .isEmpty();
        assertThat(api.statusPeriods(7, Set.of(ResidentStatus.IN_RESIDENCE), Instant.EPOCH, Instant.EPOCH))
                .isEmpty();
    }

    private ResidentStatusHistory row(ResidentStatus status, Instant at) {
        return ResidentStatusHistory.transition(7, null, status, "TEST", at, 1, false, null, null, null, null);
    }
}
