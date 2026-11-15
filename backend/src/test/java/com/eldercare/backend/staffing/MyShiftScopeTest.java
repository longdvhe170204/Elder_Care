package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import com.eldercare.backend.staffing.internal.shift.service.ShiftScheduleService;
import com.eldercare.backend.staffing.web.response.ShiftResponses;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

/** FR-060: hạn lưu ca lấy từ phân công thật và CFG, không suy từ giờ kết ca. */
@ExtendWith(MockitoExtension.class)
class MyShiftScopeTest {
    @Mock
    ObjectProvider<CurrentUser> users;

    @Mock
    ObjectProvider<AssignmentApi> assignments;

    @Mock
    CurrentUser user;

    @Mock
    AssignmentApi assignmentApi;

    @Mock
    ParameterApi parameters;

    @Test
    void FR_060_myShiftKeepsFlatShiftFieldsAndNullableScopeEnd() {
        var at = Instant.parse("2026-11-09T00:00:00Z");
        var shift = new ShiftResponses.Shift(
                20, 3, at, at.plusSeconds(3600), "CLOSED", 10L, false, List.of(), null, List.of(), 2);
        var json = JsonMapper.builder().build();
        var body = json.valueToTree(new ShiftResponses.MyShift(shift, at));
        assertThat(body.path("id").longValue()).isEqualTo(20);
        assertThat(body.path("scopeEndsAt").asString()).isEqualTo(at.toString());
        assertThat(body.has("shift")).isFalse();
        assertThat(json.valueToTree(new ShiftResponses.MyShift(shift, null))
                        .path("scopeEndsAt")
                        .isNull())
                .isTrue();
        assertThat(json.valueToTree(shift).has("scopeEndsAt")).isFalse();
    }

    @Test
    void FR_060_scopeUsesLatestOwnAssignmentAndCutsEarlyEndWithoutGrace() {
        var schedules = new ShiftScheduleService(
                null, null, null, null, null, users, assignments, null, parameters, null, null, null, null, null, null);
        var start = Instant.parse("2026-11-09T00:00:00Z");
        var end = start.plus(Duration.ofHours(8));
        var shift = new WorkShift(20, 1, 3, start, end, "FLOOR", 5L, true, "IN_PROGRESS", 10L, false, 0);
        when(users.getObject()).thenReturn(user);
        when(user.staffId()).thenReturn(Optional.of(10L));
        when(assignments.getObject()).thenReturn(assignmentApi);
        var margin = mock(ParameterValue.class);
        when(parameters.read(ParameterCode.CFG_M15_07)).thenReturn(margin);
        when(margin.asDuration()).thenReturn(Duration.ofMinutes(45));
        var early =
                new AssignmentView(AssignmentKind.SHIFT_MEMBER, 5L, null, null, 20L, start, end, end.minusSeconds(900));
        var full = new AssignmentView(AssignmentKind.SHIFT_LEAD, 5L, null, null, 20L, start, end);
        var otherShift =
                new AssignmentView(AssignmentKind.SHIFT_MEMBER, 5L, null, null, 21L, start, end.plusSeconds(3600));
        when(assignmentApi.history(10L, start, end)).thenReturn(List.of(early, full, otherShift));
        assertThat(schedules.ownShiftScopeEnd(shift)).isEqualTo(end.plusSeconds(2700));
        when(assignmentApi.history(10L, start, end)).thenReturn(List.of(early, otherShift));
        assertThat(schedules.ownShiftScopeEnd(shift)).isEqualTo(end.minusSeconds(900));
        when(assignmentApi.history(10L, start, end)).thenReturn(List.of(otherShift));
        assertThat(schedules.ownShiftScopeEnd(shift)).isNull();
    }
}
