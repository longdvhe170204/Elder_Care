package com.eldercare.backend.staffing.internal.handover.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.staff.persistence.StaffStore;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** FR-029/041/042: lệnh và checklist dùng cùng kiểm quyền, không đổi người dùng của phiên. */
@Service
public class HandoverAccess {
    private final ShiftStore shifts;
    private final StaffStore staff;
    private final DutyApi duties;
    private final PermissionApi permissions;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public HandoverAccess(
            ShiftStore shifts,
            StaffStore staff,
            DutyApi duties,
            PermissionApi permissions,
            ParameterApi parameters,
            BusinessTime time) {
        this.shifts = shifts;
        this.staff = staff;
        this.duties = duties;
        this.permissions = permissions;
        this.parameters = parameters;
        this.time = time;
    }

    public Instant preparationStarts(WorkShift shift) {
        var at = shift.endsAt().minus(parameters.read(ParameterCode.CFG_M09_04).asDuration());
        return at.isBefore(shift.startsAt()) ? shift.startsAt() : at;
    }

    public boolean inWindow(WorkShift shift) {
        var margin = parameters.read(ParameterCode.CFG_M15_07).asDuration();
        return !time.now().isBefore(shift.startsAt().minus(margin))
                && !time.now().isAfter(shift.endsAt().plus(margin));
    }

    public boolean present(WorkShift shift, CurrentUser user) {
        return user.staffId()
                .filter(id ->
                        staff.find(id).map(p -> "WORKING".equals(p.status())).orElse(false)
                                && shifts.members(shift.id()).stream()
                                        .anyMatch(m -> m.staffId() == id && m.presentAt(time.now())))
                .isPresent();
    }

    public boolean lead(WorkShift shift, CurrentUser user) {
        return !shift.missingLead()
                && user.staffId()
                        .filter(id -> Objects.equals(id, shift.leadStaffId()))
                        .isPresent()
                && present(shift, user)
                && inWindow(shift);
    }

    private boolean floor(WorkShift shift, CurrentUser user) {
        var mine = user.staffId();
        return mine.isPresent()
                && (duties.assignedFloorManager(shift.scopeId(), time.now())
                                .filter(mine.get()::equals)
                                .isPresent()
                        || duties.floorManagerSubstitute(shift.scopeId(), time.now())
                                .filter(mine.get()::equals)
                                .isPresent());
    }

    public boolean permitted(Operation operation, WorkShift shift, boolean ownMembership, CurrentUser user) {
        var check = permissions.hasPermission(
                user,
                new PermissionKey(Function.SHIFT_HANDOVER, operation),
                new Target("M09.SHIFT", Long.toString(shift.id()), null, shift.scopeId()));
        return check.allowed()
                || ownMembership
                        && check.failedLayer().filter(FailedLayer.SCOPE::equals).isPresent();
    }

    public boolean sender(WorkShift shift, CurrentUser user) {
        boolean lead = lead(shift, user);
        boolean fallback = !leadAvailable(shift) && floor(shift, user);
        return (lead || fallback) && permitted(Operation.UPDATE, shift, lead || fallback, user);
    }

    private boolean leadAvailable(WorkShift shift) {
        return shift.leadStaffId() != null
                && !shift.missingLead()
                && inWindow(shift)
                && staff.find(shift.leadStaffId())
                        .map(p -> "WORKING".equals(p.status()))
                        .orElse(false)
                && shifts.members(shift.id()).stream()
                        .anyMatch(m -> Objects.equals(m.staffId(), shift.leadStaffId()) && m.presentAt(time.now()));
    }

    public boolean editor(WorkShift shift, CurrentUser user) {
        boolean member = user.roles().contains(Role.NURSING_STAFF) && present(shift, user) && inWindow(shift);
        return sender(shift, user) || member && permitted(Operation.UPDATE, shift, true, user);
    }

    public boolean receiver(WorkShift receivingShift, CurrentUser user) {
        boolean lead = lead(receivingShift, user);
        boolean manager = floor(receivingShift, user) && present(receivingShift, user) && inWindow(receivingShift);
        return (lead || manager) && permitted(Operation.CONFIRM, receivingShift, lead || manager, user);
    }
}
