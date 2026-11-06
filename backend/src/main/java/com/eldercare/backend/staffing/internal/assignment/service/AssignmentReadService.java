package com.eldercare.backend.staffing.internal.assignment.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.staffing.internal.assignment.domain.*;
import com.eldercare.backend.staffing.internal.assignment.domain.PrimaryAssigneeResolver.TargetRef;
import com.eldercare.backend.staffing.internal.assignment.persistence.AssignmentStore;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T055: chỉ đọc, cùng bộ giải ưu tiên của API phân công. */
@Service
@Transactional(readOnly = true)
public class AssignmentReadService {
    private final ShiftStore shifts;
    private final AssignmentStore assignments;
    private final AssignmentReadSources sources;
    private final PermissionApi permissions;
    private final ScopeApi scopes;
    private final ObjectProvider<CurrentUser> users;

    public record Resolution(
            long shiftId,
            long residentId,
            Instant at,
            Role role,
            Long taskTypeId,
            Long primaryStaffId,
            List<Long> assignedNurseStaffIds) {}

    public record Missing(long residentId, boolean missingAssignedNurse, boolean missingPrimaryCaregiver) {}

    public record Unassigned(long shiftId, Instant evaluatedAt, List<Missing> items) {}

    public AssignmentReadService(
            ShiftStore shifts,
            AssignmentStore assignments,
            AssignmentReadSources sources,
            PermissionApi permissions,
            ScopeApi scopes,
            ObjectProvider<CurrentUser> users) {
        this.shifts = shifts;
        this.assignments = assignments;
        this.sources = sources;
        this.permissions = permissions;
        this.scopes = scopes;
        this.users = users;
    }

    public Resolution resolution(long shift, long resident, Instant at, Role role, Long task) {
        var s = shifts.shift(shift, false);
        requireView(s);
        requirePublished(s);
        if (at.isBefore(s.startsAt()) || !at.isBefore(s.endsAt()))
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Mốc tra cứu phải nằm trong ca");
        var refs = sources.position(resident, at);
        if (!AssignmentReadSources.inShift(s, refs) || !residentVisible(users.getObject(), resident, refs, at))
            throw new NotFoundException();
        var rows = rows(s, at);
        return new Resolution(
                shift, resident, at, role, task, primary(rows, refs, role, task, at, false), nurses(rows, refs, at));
    }

    public Unassigned unassigned(long shift) {
        var s = shifts.shift(shift, false);
        requireView(s);
        var user = users.getObject();
        boolean manager = !Collections.disjoint(user.roles(), Set.of(Role.NURSING_HOME_MANAGER, Role.FLOOR_MANAGER));
        boolean lead = !s.missingLead()
                && user.staffId()
                        .map(id -> Objects.equals(id, s.leadStaffId())
                                && shifts.members(shift).stream()
                                        .anyMatch(m -> m.staffId() == id && m.presentAt(s.startsAt())))
                        .orElse(false);
        if (!manager && !lead) throw new NotFoundException();
        requirePublished(s);
        var rows = rows(s, s.startsAt());
        var result = new ArrayList<Missing>();
        for (var e : sources.expected(s).entrySet()) {
            if (!residentVisible(user, e.getKey(), e.getValue(), s.startsAt())) throw new NotFoundException();
            boolean nurse = nurses(rows, e.getValue(), s.startsAt()).isEmpty();
            boolean caregiver = primary(rows, e.getValue(), Role.CAREGIVER, null, s.startsAt(), false) == null;
            if (nurse || caregiver) result.add(new Missing(e.getKey(), nurse, caregiver));
        }
        return new Unassigned(shift, s.startsAt(), List.copyOf(result));
    }

    private List<StaffAssignment> rows(WorkShift shift, Instant at) {
        var members = shifts.members(shift.id());
        return assignments.inShift(shift.id(), false).stream()
                .filter(a -> !Set.of("DRAFT", "CANCELLED").contains(a.status())
                        && a.activeAt(at)
                        && members.stream().anyMatch(m -> m.staffId() == a.staffId() && m.presentAt(at)))
                .toList();
    }

    private boolean residentVisible(CurrentUser user, long resident, Set<TargetRef> refs, Instant at) {
        if (scopes.historyFilter(
                        user, new PermissionKey(Function.SHIFT_SCHEDULE, Operation.VIEW), at, at.plusNanos(1000))
                .facilityWide()) return true;
        Long floor = refs.stream()
                .filter(r -> "FLOOR".equals(r.type()))
                .map(TargetRef::id)
                .findFirst()
                .orElse(null);
        return scopes.inScope(user, new Target("M01.RESIDENT", Long.toString(resident), resident, floor), at);
    }

    private List<Long> nurses(List<StaffAssignment> rows, Set<TargetRef> refs, Instant at) {
        return Optional.ofNullable(primary(rows, refs, Role.NURSING_STAFF, null, at, true)).stream()
                .toList();
    }

    private Long primary(
            List<StaffAssignment> rows, Set<TargetRef> refs, Role role, Long task, Instant at, boolean nurse) {
        try {
            return PrimaryAssigneeResolver.resolve(
                            rows.stream()
                                    .filter(a -> !nurse || a.assignedNurse())
                                    .toList(),
                            refs,
                            role,
                            task,
                            at)
                    .map(StaffAssignment::staffId)
                    .orElse(null);
        } catch (InvalidPermissionDataException e) {
            throw AssignmentReadSources.unavailable();
        }
    }

    private void requireView(WorkShift s) {
        if (!permissions
                .hasPermission(
                        users.getObject(),
                        new PermissionKey(Function.SHIFT_SCHEDULE, Operation.VIEW),
                        new Target("M09.SHIFT", Long.toString(s.id()), null, s.scopeId()))
                .allowed()) throw new NotFoundException();
    }

    private static void requirePublished(WorkShift s) {
        if (Set.of("DRAFT", "CANCELLED").contains(s.status()))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "STATE_CHANGED", "Chỉ đọc kết quả phân công của ca đã công bố");
    }
}
