package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.m07.api.dto.NursingShiftView;
import com.cambae.backend.m07.api.spi.NursingShiftPort;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
class NursingShiftLookup implements NursingShiftPort {
    private final ShiftQueries query;

    NursingShiftLookup(ShiftQueries query) {
        this.query = query;
    }

    public Optional<NursingShiftView> shiftAt(long floor, Instant at) {
        return query.atFloor(floor, at).map(s -> new NursingShiftView(s.id(), s.scopeId(), s.startsAt(), s.endsAt()));
    }

    public List<Long> nursesWithShift(long floor, Instant at) {
        return query.nurses(floor, at);
    }

    public boolean inProgressShiftMember(long staff, Instant at) {
        return query.inProgressMember(staff, at);
    }
}
