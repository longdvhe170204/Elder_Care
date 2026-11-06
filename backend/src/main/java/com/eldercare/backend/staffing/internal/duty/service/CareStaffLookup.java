package com.eldercare.backend.staffing.internal.duty.service;

import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.staffing.internal.assignment.service.AssignmentQueries;
import com.eldercare.backend.staffing.internal.handover.service.HandoverQueries;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T043c: nguồn thật của SPI mới005, giữ nguyên CareShiftPort và DTO cũ. */
@Service
@Transactional(readOnly = true)
class CareStaffLookup implements ShiftStaffPort {
    private final ShiftQueries shifts;
    private final AssignmentQueries assignments;
    private final HandoverQueries handovers;

    CareStaffLookup(ShiftQueries shifts, AssignmentQueries assignments, HandoverQueries handovers) {
        this.shifts = shifts;
        this.assignments = assignments;
        this.handovers = handovers;
    }

    public Optional<ShiftView> shift(long id) {
        return shifts.find(id).filter(s -> s.scopeId() != null).map(this::view);
    }

    private ShiftView view(WorkShift shift) {
        return new ShiftView(shift.id(), shift.scopeId(), shift.startsAt(), shift.endsAt(), shifts.published(shift));
    }

    public List<ShiftMemberView> members(long shiftId) {
        return shifts.careMembers(shiftId);
    }

    public List<ShiftView> publishedShifts(long floor, Instant from, Instant to) {
        return shifts.published(floor, from, to).stream().map(this::view).toList();
    }

    public Optional<ShiftView> shiftContaining(long floor, Instant at) {
        return shifts.atFloor(floor, at).map(this::view);
    }

    public Optional<Long> shiftLead(long id) {
        return shifts.find(id)
                .filter(shifts::published)
                .filter(s -> !s.missingLead())
                .map(WorkShift::leadStaffId);
    }

    public List<CareAssignmentView> assignments(long resident, long shift) {
        return assignments.careAssignments(resident, shift).stream()
                .map(a -> new CareAssignmentView(a.staffId(), a.role(), "PRIMARY".equals(a.assignmentKind())))
                .distinct()
                .toList();
    }

    public List<Long> responsibleNurses(long resident, Instant at) {
        return assignments.assignedNurses(resident, at);
    }

    public boolean onShift(long staff, Instant at) {
        return shifts.inProgressMember(staff, at);
    }

    public boolean qualified(long staff, Set<String> certificates, LocalDate date) {
        return shifts.qualified(staff, certificates, date);
    }

    public boolean hasQualifiedStaff(long shift, Role role, Set<String> certificates) {
        return shifts.find(shift)
                .filter(shifts::published)
                .filter(s -> shifts.anyQualified(s.id(), role, certificates))
                .isPresent();
    }

    public boolean handoverConfirmed(long previousShift) {
        return handovers.confirmed(previousShift);
    }

    public HandoverTasksView handoverTasks(long staff, long shift) {
        return handovers.tasks(staff, shift);
    }
}
