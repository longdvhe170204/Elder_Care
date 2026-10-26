package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.m04.api.dto.CareShiftView;
import com.cambae.backend.m04.api.spi.CareShiftPort;
import com.cambae.backend.m09.shift.internal.domain.WorkShift;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** Bản cài phần ca của NhanSuCaPort; không giả dữ liệu phân công hoặc bàn giao chưa có. */
@Service
class CareShiftLookup implements CareShiftPort {
    public boolean handoverConfirmed(long id) {
        return handovers.confirmed(id);
    }

    private final ShiftQueries query;
    private final com.cambae.backend.m09.handover.internal.service.HandoverQueries handovers;
    private final com.cambae.backend.m09.assignment.internal.service.AssignmentQueries assignments;

    CareShiftLookup(
            ShiftQueries query,
            com.cambae.backend.m09.assignment.internal.service.AssignmentQueries assignments,
            com.cambae.backend.m09.handover.internal.service.HandoverQueries handovers) {
        this.query = query;
        this.handovers = handovers;
        this.assignments = assignments;
    }

    public List<com.cambae.backend.m04.api.dto.CareAssignmentView> assignments(long resident, long shift) {
        return assignments.careAssignments(resident, shift);
    }

    public List<Long> assignedNurses(long resident, Instant at) {
        return assignments.assignedNurses(resident, at);
    }

    private CareShiftView view(WorkShift s) {
        return new CareShiftView(s.id(), s.scopeId(), s.startsAt(), s.endsAt(), query.published(s));
    }

    public Optional<CareShiftView> shift(long id) {
        return query.find(id).map(this::view);
    }

    public List<CareShiftView> publishedShifts(long floor, Instant from, Instant to) {
        return query.published(floor, from, to).stream().map(this::view).toList();
    }

    public Optional<CareShiftView> shiftAt(long floor, Instant at) {
        return query.atFloor(floor, at).map(this::view);
    }

    public Optional<Long> shiftLead(long id) {
        return query.find(id).filter(s -> !s.missingLead()).map(WorkShift::leadStaffId);
    }

    public boolean listedInShift(long staff, Instant at) {
        return query.inProgressMember(staff, at);
    }

    public boolean qualified(long staff, Set<String> certificates, LocalDate date) {
        return query.qualified(staff, certificates, date);
    }

    public boolean hasQualifiedStaff(long id, Role role, Set<String> certificates) {
        return query.anyQualified(id, role, certificates);
    }
}
