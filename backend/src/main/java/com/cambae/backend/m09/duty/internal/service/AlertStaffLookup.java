package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.m05.api.dto.AlertShiftView;
import com.cambae.backend.m05.api.spi.AlertStaffPort;
import com.cambae.backend.m09.shift.internal.domain.WorkShift;
import com.cambae.backend.m15.account.api.spi.DutyApi;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
class AlertStaffLookup implements AlertStaffPort {
    private final ShiftQueries query;
    private final ObjectProvider<DutyApi> duties;
    private final com.cambae.backend.m09.assignment.internal.service.AssignmentQueries assignments;

    AlertStaffLookup(
            ShiftQueries query,
            ObjectProvider<DutyApi> duties,
            com.cambae.backend.m09.assignment.internal.service.AssignmentQueries assignments) {
        this.query = query;
        this.duties = duties;
        this.assignments = assignments;
    }

    public Optional<Long> assignedNurse(long resident, Instant at) {
        return assignments.assignedNurses(resident, at).stream().findFirst();
    }

    public List<com.cambae.backend.m05.api.dto.AssignmentTraceView> assignmentsInRange(
            long resident, Instant from, Instant to) {
        return assignments.trace(resident, from, to);
    }

    private AlertShiftView view(WorkShift s) {
        return new AlertShiftView(s.id(), s.scopeId(), s.startsAt(), s.endsAt());
    }

    public Optional<AlertShiftView> shiftAt(long floor, Instant at) {
        return query.atFloor(floor, at).map(this::view);
    }

    public Optional<AlertShiftView> nextShift(long floor, Instant at) {
        return query.next(floor, at).map(this::view);
    }

    public List<Long> onDutyDoctors(Instant at) {
        return query.doctors(at);
    }

    public Optional<Long> assignedFloorManager(long floor, Instant at) {
        var api = duties.getIfAvailable();
        return api == null ? Optional.empty() : api.assignedFloorManager(floor, at);
    }

    public boolean listedInShift(long staff, long shift) {
        return query.listed(staff, shift);
    }

    public boolean inProgressShiftMember(long staff, Instant at) {
        return query.inProgressMember(staff, at);
    }

    public boolean onShift(long staff, Instant at) {
        return query.onShift(staff, at);
    }

    public Optional<Long> floorDuringShift(long staff, Instant at) {
        return query.floor(staff, at);
    }

    public List<Long> floorsWithShift(Instant at) {
        return query.floors(at);
    }
}
