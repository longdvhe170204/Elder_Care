package com.cambae.backend.m09.shift.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.LocationStructureApi;
import com.cambae.backend.m09.assignment.internal.persistence.AssignmentStore;
import com.cambae.backend.m09.shift.internal.persistence.*;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quyền trường và phạm vi nhật ký dùng cùng định danh dòng vắng; không trả lý do qua SPI. */
@Service
@Transactional(readOnly = true)
public class AbsenceReasonAccess implements StaffAbsenceApi, ScopeAssignmentProvider {
    private final PostPublicationStore store;
    private final ShiftStore shifts;
    private final AssignmentStore assignments;
    private final BusinessTime time;
    private final ObjectProvider<LocationStructureApi> structures;

    public AbsenceReasonAccess(
            PostPublicationStore store,
            ShiftStore shifts,
            AssignmentStore assignments,
            BusinessTime time,
            ObjectProvider<LocationStructureApi> structures) {
        this.store = store;
        this.shifts = shifts;
        this.assignments = assignments;
        this.time = time;
        this.structures = structures;
    }

    public String entityType() {
        return "M09.STAFF_ABSENCE";
    }

    public Target assignScope(long id) {
        var row = store.absence(id).orElseThrow(NotFoundException::new);
        return new Target(
                entityType(),
                Long.toString(id),
                null,
                shifts.shift(row.shiftId(), false).scopeId());
    }

    public boolean canViewReason(CurrentUser user, long absenceId) {
        var row = store.absence(absenceId).orElse(null);
        if (row == null) return false;
        if (user.roles().contains(Role.NURSING_HOME_MANAGER) || Objects.equals(row.recordedBy(), user.accountId()))
            return true;
        if (!user.roles().contains(Role.FLOOR_MANAGER) || user.staffId().isEmpty()) return false;
        var s = shifts.find(row.shiftId()).orElse(null);
        if (s == null || s.scopeId() == null) return false;
        if (head(user, s.scopeId())) return true;
        var api = structures.getIfAvailable();
        if (api == null) return false;
        if ("FLOOR".equals(s.scopeType()))
            return api.floor(s.scopeId()).map(f -> head(user, f.areaId())).orElse(false);
        return "AREA".equals(s.scopeType())
                && api.floorsInArea(s.scopeId()).stream().anyMatch(f -> head(user, f));
    }

    private boolean head(CurrentUser user, long scope) {
        var now = time.now();
        return assignments.managers(scope, false).stream()
                .anyMatch(m -> m.staffId() == user.staffId().orElseThrow()
                        && !m.startsAt().isAfter(now)
                        && (m.endsAt() == null || m.endsAt().isAfter(now)));
    }
}
