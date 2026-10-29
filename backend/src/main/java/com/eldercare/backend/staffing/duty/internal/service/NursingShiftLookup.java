package com.eldercare.backend.staffing.duty.internal.service;

import com.eldercare.backend.medication.api.dto.NursingShiftView;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
class NursingShiftLookup implements NursingShiftPort, com.eldercare.backend.medication.api.spi.ShiftNursePort {
    public Optional<com.eldercare.backend.medication.api.spi.ShiftView> shiftContaining(long floor, Instant at) {
        return query.atFloor(floor, at)
                .map(shift -> new com.eldercare.backend.medication.api.spi.ShiftView(
                        shift.id(),
                        floor,
                        shift.startsAt(),
                        shift.endsAt(),
                        query.leadAtFloor(floor, at).orElse(null)));
    }

    public Optional<Long> assignedNurse(long resident, Instant at) {
        return assignedTo(resident, at);
    }

    public List<Long> nursesOnShift(long floor, Instant at) {
        return nursesWithShift(floor, at);
    }

    public boolean isStaffOnActiveShift(long staff, Instant at) {
        return inProgressShiftMember(staff, at);
    }

    public boolean isHandoverConfirmed(long shift) {
        return handoverConfirmed(shift);
    }

    public boolean handoverConfirmed(long id) {
        return handovers.confirmed(id);
    }

    private final ShiftQueries query;
    private final com.eldercare.backend.staffing.handover.internal.service.HandoverQueries handovers;
    private final com.eldercare.backend.staffing.assignment.internal.service.AssignmentQueries assignments;

    NursingShiftLookup(
            ShiftQueries query,
            com.eldercare.backend.staffing.assignment.internal.service.AssignmentQueries assignments,
            com.eldercare.backend.staffing.handover.internal.service.HandoverQueries handovers) {
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
