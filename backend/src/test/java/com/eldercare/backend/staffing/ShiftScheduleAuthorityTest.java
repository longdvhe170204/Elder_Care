package com.eldercare.backend.staffing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.internal.assignment.service.AssignmentService;
import com.eldercare.backend.staffing.internal.shift.domain.*;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.shift.service.ShiftScheduleService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/** Quyết định 19/11: tạo khung tháng và điều hành ca thuộc hai người khác nhau. */
@ExtendWith(MockitoExtension.class)
class ShiftScheduleAuthorityTest {
    @Mock
    ShiftStore store;

    @Mock
    com.eldercare.backend.staffing.internal.staff.persistence.StaffStore staff;

    @Mock
    PermissionApi permissions;

    @Mock
    ObjectProvider<CurrentUser> users;

    @Mock
    CurrentUser user;

    @Mock
    BusinessTime time;

    @Mock
    AssignmentService assignments;

    @InjectMocks
    ShiftScheduleService service;

    @BeforeEach
    void setup() {
        when(users.getObject()).thenReturn(user);
        lenient()
                .when(permissions.hasPermission(any(), any(), any()))
                .thenReturn(new PermissionResult(true, Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    void FR_012_onlyManagerCreatesMonthlySchedule() {
        when(user.roles()).thenReturn(Set.of(Role.FLOOR_MANAGER));
        assertThrows(
                PermissionDeniedException.class,
                () -> service.createSchedule(
                        YearMonth.of(2027, 1), "FLOOR", 7L, new Target("M09.SCHEDULE", null, null, 7L)));
        verifyNoInteractions(store);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        assertDoesNotThrow(() ->
                service.createSchedule(YearMonth.of(2027, 1), "FLOOR", 7L, new Target("M09.SCHEDULE", null, null, 7L)));
    }

    @Test
    void FR_012_managerCannotEditFloorShiftEvenWithBroadPermission() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        var shift = new WorkShift(
                1, 2, 3, Instant.EPOCH, Instant.EPOCH.plusSeconds(3600), "FLOOR", 7L, true, "DRAFT", null, false, 0);
        when(store.shift(1, false)).thenReturn(shift);
        when(store.shift(1, true)).thenReturn(shift);
        when(store.members(1)).thenReturn(List.of());
        when(store.schedule(2, true))
                .thenReturn(new ShiftSchedule(2, YearMonth.of(2027, 1), "FLOOR", 7L, "DRAFT", 10L, 0));
        assertThrows(
                PermissionDeniedException.class,
                () -> service.deleteShift(1, "\"0\"", new Target("M09.SCHEDULE", null, null, 7L)));
        verify(store, never()).deleteShift(any());
        assertFalse(service.canEditShift(shift));
        when(user.roles()).thenReturn(Set.of(Role.FLOOR_MANAGER));
        assertTrue(service.canEditShift(shift));
        assertDoesNotThrow(() -> service.deleteShift(1, "\"0\"", Target.UNSPECIFIED));
        verify(store).deleteShift(shift);
        when(permissions.hasPermission(any(), any(), any()))
                .thenReturn(new PermissionResult(false, Optional.empty(), Optional.empty(), Optional.empty()));
        assertThrows(PermissionDeniedException.class, () -> service.deleteShift(1, "\"0\"", Target.UNSPECIFIED));
        verify(store, times(1)).deleteShift(shift);
        when(permissions.hasPermission(any(), any(), any()))
                .thenReturn(new PermissionResult(true, Optional.empty(), Optional.empty(), Optional.empty()));
        var facilityShift = new WorkShift(
                1,
                2,
                3,
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(3600),
                "FACILITY",
                null,
                false,
                "DRAFT",
                null,
                false,
                0);
        when(store.shift(1, false)).thenReturn(facilityShift);
        when(store.shift(1, true)).thenReturn(facilityShift);
        when(store.schedule(2, true))
                .thenReturn(new ShiftSchedule(2, YearMonth.of(2027, 1), "FACILITY", null, "DRAFT", 10L, 0));
        assertThrows(PermissionDeniedException.class, () -> service.deleteShift(1, "\"0\"", Target.UNSPECIFIED));
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        assertDoesNotThrow(() -> service.deleteShift(1, "\"0\"", Target.UNSPECIFIED));
        verify(store).deleteShift(facilityShift);
    }

    @Test
    void FR_016_managerCannotPublishFloorMonthlySchedule() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        when(store.scheduleShifts(2)).thenReturn(List.of());
        when(store.schedule(2, true))
                .thenReturn(new ShiftSchedule(2, YearMonth.of(2027, 1), "FLOOR", 7L, "DRAFT", 10L, 0));
        assertThrows(
                PermissionDeniedException.class, () -> service.publish(2, List.of(), false, null, Target.UNSPECIFIED));
        when(user.roles()).thenReturn(Set.of(Role.FLOOR_MANAGER));
        when(store.schedule(2, true))
                .thenReturn(new ShiftSchedule(2, YearMonth.of(2027, 1), "FACILITY", null, "DRAFT", 10L, 0));
        assertThrows(
                PermissionDeniedException.class, () -> service.publish(2, List.of(), false, null, Target.UNSPECIFIED));
    }
}
