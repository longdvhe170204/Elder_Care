package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.internal.handover.service.HandoverAccess;
import com.eldercare.backend.staffing.internal.shift.domain.ShiftMember;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.staff.domain.StaffProfile;
import com.eldercare.backend.staffing.internal.staff.persistence.StaffStore;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Q-417 (spec 008 Cập nhật 2026-11-22): Trưởng tầng xác nhận thay khi người phụ trách ca sau vắng hoặc quá hạn. */
class Q_417_HandoverSubstituteTest {
    private static final long FLOOR = 7, LEAD = 11, MANAGER = 12, OTHER = 13;
    private static final Instant STARTS = Instant.parse("2035-05-01T07:00:00Z");
    private static final WorkShift NEXT = new WorkShift(
            99, 1, 1, STARTS, STARTS.plus(Duration.ofHours(8)), "FLOOR", FLOOR, true, "IN_PROGRESS", LEAD, false, 0);

    private final ShiftStore shifts = mock(ShiftStore.class);
    private final StaffStore staff = mock(StaffStore.class);
    private final DutyApi duties = mock(DutyApi.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final ParameterApi parameters = mock(ParameterApi.class);

    Q_417_HandoverSubstituteTest() {
        var working = mock(StaffProfile.class);
        when(working.status()).thenReturn("WORKING");
        when(staff.find(anyLong())).thenReturn(Optional.of(working));
        when(duties.assignedFloorManager(eq(FLOOR), any())).thenReturn(Optional.of(MANAGER));
        when(duties.floorManagerSubstitute(eq(FLOOR), any())).thenReturn(Optional.empty());
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        duration(ParameterCode.CFG_M09_05, Duration.ofMinutes(30));
        duration(ParameterCode.CFG_M15_07, Duration.ofMinutes(30));
        leadPresent(true);
    }

    private void duration(ParameterCode code, Duration value) {
        var parameter = mock(ParameterValue.class);
        when(parameter.asDuration()).thenReturn(value);
        when(parameters.read(code)).thenReturn(parameter);
    }

    private void leadPresent(boolean present) {
        when(shifts.members(NEXT.id()))
                .thenReturn(List.of(new ShiftMember(LEAD, false, present ? null : STARTS, null, null, null, null)));
    }

    private HandoverAccess at(Instant now) {
        return new HandoverAccess(
                shifts, staff, duties, permissions, parameters, new BusinessTime(Clock.fixed(now, ZoneOffset.UTC)));
    }

    private static CurrentUser user(long staffId, Role role) {
        var user = mock(CurrentUser.class);
        when(user.staffId()).thenReturn(Optional.of(staffId));
        when(user.roles()).thenReturn(Set.of(role));
        return user;
    }

    @Test
    void Q_417_floorManagerWaitsWhileNextLeadIsPresentAndOnTime() {
        var access = at(STARTS.plus(Duration.ofMinutes(10)));
        assertThat(access.receiver(NEXT, user(LEAD, Role.NURSING_STAFF))).isTrue();
        assertThat(access.receiver(NEXT, user(MANAGER, Role.FLOOR_MANAGER))).isFalse();
    }

    @Test
    void Q_417_floorManagerConfirmsAfterConfirmDelayWithoutBeingInNextShift() {
        var access = at(STARTS.plus(Duration.ofMinutes(30)));
        assertThat(access.receiver(NEXT, user(MANAGER, Role.FLOOR_MANAGER))).isTrue();
        assertThat(access.receiver(NEXT, user(OTHER, Role.FLOOR_MANAGER))).isFalse();
    }

    @Test
    void Q_417_floorManagerConfirmsWhenNextLeadIsAbsent() {
        leadPresent(false);
        assertThat(at(STARTS.plus(Duration.ofMinutes(5))).receiver(NEXT, user(MANAGER, Role.FLOOR_MANAGER)))
                .isTrue();
        assertThat(at(STARTS.plus(Duration.ofMinutes(5))).receiver(NEXT, user(LEAD, Role.NURSING_STAFF)))
                .isFalse();
    }

    @Test
    void Q_417_noSubstituteBeforeNextShiftStarts() {
        leadPresent(false);
        assertThat(at(STARTS.minus(Duration.ofMinutes(5))).receiver(NEXT, user(MANAGER, Role.FLOOR_MANAGER)))
                .isFalse();
    }
}
