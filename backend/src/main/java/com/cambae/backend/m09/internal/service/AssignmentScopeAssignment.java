package com.cambae.backend.m09.internal.service;

import com.cambae.backend.m09.assignment.internal.persistence.AssignmentStore;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;

/** Phạm vi bản ghi phân công là ca quản lý nó; giao Trưởng tầng là tầng/khu được giao. */
public class AssignmentScopeAssignment implements ScopeAssignmentProvider {
    private final String type;
    private final AssignmentStore assignments;
    private final ShiftStore shifts;

    public AssignmentScopeAssignment(String type, AssignmentStore assignments, ShiftStore shifts) {
        this.type = type;
        this.assignments = assignments;
        this.shifts = shifts;
    }

    public String entityType() {
        return type;
    }

    public Target assignScope(long id) {
        if ("M09.FLOOR_MANAGER_ASSIGNMENT".equals(type)) {
            var a = assignments.manager(id, false);
            return new Target(type, Long.toString(id), null, a.floorAreaId());
        }
        var a = assignments.find(id, false);
        var s = shifts.shift(a.shiftId(), false);
        return new Target(
                type, Long.toString(id), "RESIDENT".equals(a.targetType()) ? a.targetId() : null, s.scopeId());
    }
}
