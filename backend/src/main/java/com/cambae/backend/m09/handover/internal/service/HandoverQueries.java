package com.cambae.backend.m09.handover.internal.service;

import com.cambae.backend.m09.handover.internal.persistence.HandoverStore;
import com.cambae.backend.m09.shift.internal.service.port.ShiftHandoverState;
import java.util.Set;
import org.springframework.stereotype.Service;

/** T026: nguồn thật cho vòng đời ca; Có ý kiến vẫn là chưa lập xong (BR-M09-07). */
@Service
public class HandoverQueries implements ShiftHandoverState {
    private final HandoverStore store;
    private final com.cambae.backend.m09.shift.internal.persistence.ShiftStore shifts;
    private final com.cambae.backend.m15.parameter.api.ParameterApi parameters;
    private final com.cambae.backend.common.time.BusinessTime time;

    public HandoverQueries(
            HandoverStore store,
            com.cambae.backend.m09.shift.internal.persistence.ShiftStore shifts,
            com.cambae.backend.m15.parameter.api.ParameterApi parameters,
            com.cambae.backend.common.time.BusinessTime time) {
        this.store = store;
        this.shifts = shifts;
        this.parameters = parameters;
        this.time = time;
    }

    @Override
    public boolean prepared(long shiftId) {
        return store.statusForShift(shiftId)
                .filter(Set.of("PREPARED", "CONFIRMED")::contains)
                .isPresent();
    }

    public boolean confirmed(long shiftId) {
        return store.statusForShift(shiftId).filter("CONFIRMED"::equals).isPresent();
    }

    public java.util.List<com.cambae.backend.m05.api.dto.UnconfirmedShiftHandoverView> startedUnconfirmed(
            java.time.Instant from, java.time.Instant to) {
        var gap = parameters
                .read(com.cambae.backend.m15.parameter.api.ParameterCode.CFG_M09_09)
                .asDuration();
        var result = new java.util.ArrayList<com.cambae.backend.m05.api.dto.UnconfirmedShiftHandoverView>();
        for (long id : store.unconfirmedPrevious(from.minus(gap), to)) {
            var previous = shifts.shift(id, false);
            var next = com.cambae.backend.m09.handover.internal.domain.NextHandoverShift.find(
                    previous,
                    shifts.floorRange(
                            previous.scopeId(),
                            previous.startsAt(),
                            previous.endsAt().plus(gap).plusNanos(1000)),
                    gap);
            next.filter(s -> s.startsAt().isAfter(from)
                            && !s.startsAt().isAfter(to)
                            && !s.startsAt().isAfter(time.now()))
                    .ifPresent(s -> result.add(new com.cambae.backend.m05.api.dto.UnconfirmedShiftHandoverView(
                            previous.id(), s.id(), previous.scopeId())));
        }
        return java.util.List.copyOf(result);
    }
}
