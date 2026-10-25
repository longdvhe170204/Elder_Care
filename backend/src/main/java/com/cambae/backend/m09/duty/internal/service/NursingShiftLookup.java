package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.m07.api.dto.NursingShiftView;
import com.cambae.backend.m07.api.spi.NursingShiftPort;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
class NursingShiftLookup implements NursingShiftPort {
    public boolean handoverConfirmed(long id) {
        return handovers.confirmed(id);
    }

    private final ShiftQueries query;
    private final com.cambae.backend.m09.handover.internal.service.HandoverQueries handovers;
    private final com.cambae.backend.m09.assignment.internal.service.AssignmentQueries assignments;

    NursingShiftLookup(
            ShiftQueries query,
            com.cambae.backend.m09.assignment.internal.service.AssignmentQueries assignments,
            com.cambae.backend.m09.handover.internal.service.HandoverQueries handovers) {
        this.query = query;
        this.handovers = handovers;
        this.assignments = assignments;
    }

    public Optional<Long> assignedTo(long resident, Instant at) {
        return assignments.assignedNurses(resident, at).stream().findFirst();
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
