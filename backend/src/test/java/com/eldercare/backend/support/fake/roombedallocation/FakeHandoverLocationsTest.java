package com.eldercare.backend.support.fake.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.roombedallocation.api.dto.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FakeHandoverLocationsTest {
    private final FakeHandoverLocations source = new FakeHandoverLocations();
    private final Instant start = Instant.parse("2026-10-30T00:00:00Z");

    @Test
    void FR_024_stateCarriesTheRoomSuppliedByTheFixture() {
        source.beds(10, List.of(new AwaitingReadyBedView(20, 30, "Giường chờ", start)));
        assertThat(source.stateAt(ScopeFilter.FACILITY, start)).singleElement().satisfies(row -> {
            assertThat(row.roomId()).isEqualTo(30);
            assertThat(row.holdReason()).isNull();
        });
    }

    @Test
    void FR_024_heldFixturePreservesItsReasonAndRoomScope() {
        source.state(new BedStateOnDate(20, 10, BedStatus.HELD, start, 30, "ABSENT_RESIDENT"));
        var room = new ScopeFilter(false, Set.of(), Set.of(30L), Set.of());
        assertThat(source.stateAt(room, start.plusSeconds(1))).singleElement().satisfies(row -> {
            assertThat(row.roomId()).isEqualTo(30);
            assertThat(row.holdReason()).isEqualTo("ABSENT_RESIDENT");
        });
        source.state(20, "AVAILABLE");
        assertThat(source.stateAt(room, start.plusSeconds(2)))
                .singleElement()
                .satisfies(row -> assertThat(row.holdReason()).isNull());
    }

    @Test
    void FR_024_heldStateWithoutAReasonDoesNotInventReportData() {
        source.beds(10, List.of(new AwaitingReadyBedView(20, 30, "Giường chờ", start)));
        source.state(20, "HELD");
        assertThatThrownBy(() -> source.stateAt(ScopeFilter.FACILITY, start).toList())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void FR_024_missingHoldReasonOutsideTheRequestedFloorDoesNotBreakTheReport() {
        source.beds(10, List.of(new AwaitingReadyBedView(20, 30, "Giường chờ", start)));
        source.beds(11, List.of(new AwaitingReadyBedView(21, 31, "Giường tầng khác", start)));
        source.state(21, "HELD");
        assertThat(source.stateAt(new ScopeFilter(false, Set.of(10L), Set.of(), Set.of()), start))
                .singleElement()
                .satisfies(row -> assertThat(row.bedId()).isEqualTo(20));
        assertThatThrownBy(() -> source.stateAt(new ScopeFilter(false, Set.of(11L), Set.of(), Set.of()), start)
                        .toList())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void Q_390_readinessWindowClipsItsStartAndKeepsTheOriginalStart() {
        source.beds(10, List.of(new AwaitingReadyBedView(20, 30, "Giường chờ", start)));
        Instant from = start.plusSeconds(60), to = start.plusSeconds(120);
        assertThat(source.awaitingReady(ScopeFilter.FACILITY, from, to))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.from()).isEqualTo(from);
                    assertThat(row.to()).isEqualTo(to);
                    assertThat(row.startedAt()).isEqualTo(start);
                });
    }

    @Test
    void Q_390_readinessWindowExcludesFutureAndTouchingIntervals() {
        source.beds(10, List.of(new AwaitingReadyBedView(20, 30, "Giường chờ", start)));
        assertThat(source.awaitingReady(ScopeFilter.FACILITY, start.minusSeconds(120), start.minusSeconds(60)))
                .isEmpty();
        assertThat(source.awaitingReady(ScopeFilter.FACILITY, start.minusSeconds(60), start))
                .isEmpty();
        assertThat(source.awaitingReady(
                        new ScopeFilter(false, Set.of(11L), Set.of(), Set.of()), start, start.plusSeconds(60)))
                .isEmpty();
    }
}
