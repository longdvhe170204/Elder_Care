package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.api.spi.TripDutyPort;
import com.cambae.backend.m09.assignment.internal.service.AssignmentQueries;
import com.cambae.backend.m15.account.api.spi.DutyApi;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class DutyDirectory implements DutyApi {
    private final ShiftQueries shifts;
    private final AssignmentQueries assignments;
    private final DepartmentDutyService departments;
    private final ObjectProvider<TripDutyPort> trips;
    private final BusinessTime time;

    public DutyDirectory(
            ShiftQueries shifts,
            AssignmentQueries assignments,
            DepartmentDutyService departments,
            ObjectProvider<TripDutyPort> trips,
            BusinessTime time) {
        this.shifts = shifts;
        this.assignments = assignments;
        this.departments = departments;
        this.trips = trips;
        this.time = time;
    }

    public Optional<Long> shiftLead(long floor, Instant at) {
        return shifts.leadAtFloor(floor, at);
    }

    public Optional<Long> assignedFloorManager(long floor, Instant at) {
        return assignments.floorManager(floor, at);
    }

    public Optional<Long> floorManagerSubstitute(long floor, Instant at) {
        return assignedFloorManager(floor, at).isPresent() ? Optional.empty() : shiftLead(floor, at);
    }

    public boolean shiftHasNoFloorManager(long shift) {
        return shifts.find(shift)
                .map(s -> s.scopeId() != null
                        && assignedFloorManager(s.scopeId(), time.now()).isEmpty())
                .orElse(false);
    }

    public Optional<Long> departmentLead(long id) {
        return departments.lead(id);
    }

    public boolean isDepartmentLead(long id) {
        return departments.isLead(id);
    }

    public Optional<Long> departmentOf(long id, Instant at) {
        return departments.departmentOf(id, at);
    }

    public Optional<Long> groupLeader(long id) {
        var p = trips.getIfAvailable();
        return p == null ? Optional.empty() : p.groupLeader(id);
    }

    public List<Long> companions(long id) {
        var p = trips.getIfAvailable();
        return p == null ? List.of() : p.companions(id);
    }

    public List<Long> floorReceivers(long floor, long shift) {
        return assignments.floorReceivers(floor, shift);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void replaceDepartmentLead(long old, long replacement, Reason reason) {
        departments.replaceLead(old, replacement, reason);
    }
}
