package com.eldercare.backend.staffing;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.shift.service.ShiftHandoverState;
import com.eldercare.backend.staffing.internal.shift.service.ShiftLifecycle;
import java.time.*;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/** Q-417 (spec 008 FR-061): ca không còn công việc chưa đóng hay cảnh báo mở thì đóng ca không chờ bàn giao. */
class Q_417_ConditionalHandoverTest {
    private static final Instant ENDS = Instant.parse("2035-05-01T15:00:00Z");
    private static final WorkShift SHIFT = new WorkShift(
            5, 1, 1, ENDS.minus(Duration.ofHours(8)), ENDS, "FLOOR", 7L, true, "IN_PROGRESS", 11L, false, 0);

    private final ShiftStore shifts = mock(ShiftStore.class);
    private final ShiftHandoverState state = mock(ShiftHandoverState.class);

    @SuppressWarnings("unchecked")
    private ShiftLifecycle lifecycle() {
        ObjectProvider<ShiftHandoverState> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenAnswer(i -> Stream.of(state));
        when(shifts.due(any())).thenReturn(List.of(SHIFT));
        when(shifts.shift(SHIFT.id(), true)).thenReturn(SHIFT);
        return new ShiftLifecycle(shifts, provider, mock(AuditLogApi.class), mock(ApplicationEventPublisher.class));
    }

    @Test
    void Q_417_nothingOutstandingClosesShiftWithoutHandover() {
        var lifecycle = lifecycle();
        when(state.prepared(SHIFT.id())).thenReturn(false);
        when(state.mandatory(SHIFT.id())).thenReturn(false);
        lifecycle.run(ENDS, ENDS);
        verify(shifts).transition(SHIFT.id(), "CLOSED");
    }

    @Test
    void Q_417_outstandingWorkKeepsShiftAwaitingHandover() {
        var lifecycle = lifecycle();
        when(state.prepared(SHIFT.id())).thenReturn(false);
        when(state.mandatory(SHIFT.id())).thenReturn(true);
        lifecycle.run(ENDS, ENDS);
        verify(shifts).transition(SHIFT.id(), "AWAITING_HANDOVER");
    }
}
