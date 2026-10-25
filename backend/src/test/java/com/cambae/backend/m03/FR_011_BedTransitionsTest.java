package com.cambae.backend.m03;

import static com.cambae.backend.m03.api.dto.BedStatus.*;
import static com.cambae.backend.m03.internal.bed.domain.BedEvent.*;
import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.m03.api.dto.BedStatus;
import com.cambae.backend.m03.internal.bed.domain.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class FR_011_BedTransitionsTest {
    record Row(
            BedStatus from, HoldReason hold, BedEvent event, BedStatus maintenance, boolean reserved, BedStatus to) {}

    static List<Row> rows() {
        var rows = new ArrayList<Row>();
        rows.add(new Row(null, null, CREATE_BED, null, false, EMPTY));
        rows.add(new Row(EMPTY, null, ALLOCATION_STARTED, null, false, OCCUPIED));
        rows.add(new Row(EMPTY, null, HOLD_WAITING_RECORD, null, false, HELD));
        rows.add(new Row(HELD, HoldReason.WAITING_RECORD, ALLOCATION_STARTED, null, false, OCCUPIED));
        for (var event : List.of(HOLD_EXPIRED, HOLD_CANCELLED))
            rows.add(new Row(HELD, HoldReason.WAITING_RECORD, event, null, false, EMPTY));
        rows.add(new Row(OCCUPIED, null, ABSENT_KEEP_BED, null, false, HELD));
        rows.add(new Row(HELD, HoldReason.ABSENT_RESIDENT, RETURNED, null, false, OCCUPIED));
        for (var event : List.of(RELEASE_WHEN_ABSENT, RESIDENCY_ENDED, DEATH, TRANSFERRED_OUT))
            rows.add(new Row(HELD, HoldReason.ABSENT_RESIDENT, event, null, false, AWAITING_READY));
        for (var event : List.of(TRANSFERRED_OUT, RESIDENCY_ENDED, DEATH, ABSENT_RELEASE_BED))
            rows.add(new Row(OCCUPIED, null, event, null, false, AWAITING_READY));
        rows.add(new Row(AWAITING_READY, null, CONFIRM_READY, null, false, EMPTY));
        rows.add(new Row(AWAITING_READY, null, CONFIRM_READY, null, true, OCCUPIED));
        for (var from : List.of(EMPTY, AWAITING_READY, SUSPENDED))
            rows.add(new Row(from, null, ASSET_MAINTENANCE, null, false, UNDER_MAINTENANCE));
        for (var from : List.of(EMPTY, AWAITING_READY, SUSPENDED, UNDER_MAINTENANCE))
            rows.add(new Row(from, null, ASSET_RETIRED, null, false, RETIRED));
        for (var previous : List.of(EMPTY, AWAITING_READY, SUSPENDED))
            rows.add(new Row(
                    UNDER_MAINTENANCE, null, ASSET_READY, previous, false, previous == SUSPENDED ? SUSPENDED : EMPTY));
        rows.add(new Row(EMPTY, null, SUSPEND, null, false, SUSPENDED));
        rows.add(new Row(SUSPENDED, null, REUSE, null, false, AWAITING_READY));
        return rows;
    }

    static Stream<Row> allowed() {
        return rows().stream();
    }

    @ParameterizedTest
    @MethodSource("allowed")
    void FR_011_allDeclaredTransitionsMatchTheTable(Row row) {
        assertThat(BedTransitions.next(row.from(), row.hold(), row.event(), row.maintenance(), row.reserved()))
                .isEqualTo(row.to());
    }

    static Stream<Row> rejected() {
        var contexts = new ArrayList<BedStatus>(Arrays.asList(BedStatus.values()));
        contexts.add(null);
        var invalid = new ArrayList<Row>();
        for (var from : contexts)
            for (var hold : Arrays.asList(null, HoldReason.WAITING_RECORD, HoldReason.ABSENT_RESIDENT)) {
                if (from != HELD && hold != null) continue;
                for (var event : BedEvent.values()) {
                    var row = new Row(from, hold, event, null, false, null);
                    if (rows().stream()
                            .noneMatch(valid -> valid.from() == from
                                    && valid.hold() == hold
                                    && valid.event() == event
                                    && valid.maintenance() == null
                                    && !valid.reserved())) invalid.add(row);
                }
            }
        return invalid.stream();
    }

    @ParameterizedTest
    @MethodSource("rejected")
    void FR_011_everyOtherStateEventPairIsRejected(Row row) {
        assertThatThrownBy(() -> BedTransitions.next(row.from(), row.hold(), row.event(), null, false))
                .isInstanceOf(IllegalStateException.class);
    }
}
