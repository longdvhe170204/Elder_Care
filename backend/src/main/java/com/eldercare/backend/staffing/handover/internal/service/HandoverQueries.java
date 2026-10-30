package com.eldercare.backend.staffing.handover.internal.service;

import com.eldercare.backend.staffing.handover.internal.persistence.HandoverStore;
import com.eldercare.backend.staffing.shift.internal.service.port.ShiftHandoverState;
import java.util.Set;
import org.springframework.stereotype.Service;

/** T026: nguồn thật cho vòng đời ca; Có ý kiến vẫn là chưa lập xong (BR-M09-07). */
@Service
public class HandoverQueries implements ShiftHandoverState {
    private final HandoverStore store;
    private final com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore shifts;
    private final com.eldercare.backend.administration.parameter.api.ParameterApi parameters;
    private final com.eldercare.backend.common.time.BusinessTime time;

    public HandoverQueries(
            HandoverStore store,
            com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore shifts,
            com.eldercare.backend.administration.parameter.api.ParameterApi parameters,
            com.eldercare.backend.common.time.BusinessTime time) {
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

    public java.util.List<com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView> startedUnconfirmed(
            java.time.Instant from, java.time.Instant to) {
        var gap = parameters
                .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M09_09)
                .asDuration();
        var result = new java.util.ArrayList<com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView>();
        for (long id : store.unconfirmedPrevious(from.minus(gap), to)) {
            var previous = shifts.shift(id, false);
            var next = com.eldercare.backend.staffing.handover.internal.domain.NextHandoverShift.find(
                    previous,
                    shifts.floorRange(
                            previous.scopeId(),
                            previous.startsAt(),
                            previous.endsAt().plus(gap).plusNanos(1000)),
                    gap);
            next.filter(s -> s.startsAt().isAfter(from)
                            && !s.startsAt().isAfter(to)
                            && !s.startsAt().isAfter(time.now()))
                    .ifPresent(s -> result.add(new com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView(
                            previous.id(), s.id(), previous.scopeId())));
        }
        return java.util.List.copyOf(result);
    }
}
