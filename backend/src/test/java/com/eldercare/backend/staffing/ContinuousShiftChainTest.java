package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.eldercare.backend.staffing.shift.internal.domain.ContinuousShiftChain;
import com.eldercare.backend.staffing.shift.internal.domain.ContinuousShiftChain.Interval;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** T010, BR-M09-03: ca tiếp giáp nối thành một chuỗi; ngưỡng do service đọc từ CFG-M09-02. */
class ContinuousShiftChainTest {
    private final Instant origin = Instant.parse("2026-10-31T17:00:00Z");

    private Interval hours(long from, long to) {
        return new Interval(origin.plus(Duration.ofHours(from)), origin.plus(Duration.ofHours(to)));
    }

    @Test
    void BR_M09_03_newShiftBridgesBothSidesRegardlessOfInputOrder() {
        assertThat(ContinuousShiftChain.duration(List.of(hours(16, 24), hours(0, 8)), hours(8, 16)))
                .isEqualTo(Duration.ofHours(24));
    }

    @Test
    void BR_M09_03_exactThresholdIsNotExceededAndDoesNotRoundAwayExtraTime() {
        Duration threshold = Duration.ofHours(16);
        assertThat(ContinuousShiftChain.duration(List.of(hours(0, 8)), hours(8, 16))
                        .compareTo(threshold))
                .isZero();
        var extra = new Interval(hours(8, 16).from(), hours(8, 16).to().plusNanos(1));
        assertThat(ContinuousShiftChain.duration(List.of(hours(0, 8)), extra).compareTo(threshold))
                .isPositive();
    }

    @Test
    void BR_M09_03_realGapBreaksTheChainEvenAcrossMonthBoundary() {
        var separated =
                new Interval(hours(8, 16).from().plusNanos(1), hours(8, 16).to());
        assertThat(ContinuousShiftChain.duration(List.of(hours(0, 8)), separated))
                .isEqualTo(Duration.ofHours(8).minusNanos(1));
    }

    @Test
    void BR_M09_03_unrelatedLongerChainIsNotAttributedToNewShift() {
        assertThat(ContinuousShiftChain.duration(List.of(hours(-48, -24), hours(-24, -8)), hours(0, 8)))
                .isEqualTo(Duration.ofHours(8));
    }

    @Test
    void BR_M09_03_overlappingIntervalsCountElapsedTimeOnceWithoutMutatingInput() {
        var existing = List.of(hours(4, 12), hours(0, 8));
        assertThat(ContinuousShiftChain.duration(existing, hours(10, 16))).isEqualTo(Duration.ofHours(16));
        assertThat(existing).containsExactly(hours(4, 12), hours(0, 8));
    }

    @Test
    void BR_M09_12_emptyAndReversedIntervalsAreRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> hours(8, 8));
        assertThatIllegalArgumentException().isThrownBy(() -> hours(9, 8));
    }
}
