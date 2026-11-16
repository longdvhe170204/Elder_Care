package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.events.MeasurementTaskMissed;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskClockProcessor;
import com.eldercare.backend.care.internal.task.service.CareTaskNotices;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 005 FR-026, FR-044; 007 FR-039b, Q-75 (chủ dự án chốt 2026-11-16): công việc căn cứ lịch đo đóng Không thực hiện hoặc
 * còn Quá hạn lúc hết ca thì 005 phát {@code MeasurementTaskMissed} một lần, kèm mã lịch đo, {@code vitalSignIds} trống.
 */
class FR_044_MeasurementTaskMissedTest {

    private static final Instant SHIFT_END = Instant.parse("2026-11-02T11:00:00Z");

    private final ReminderMarkers markers = mock(ReminderMarkers.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final CareTaskNotices notices = new CareTaskNotices(markers, events, mock(StatusHistoryApi.class));

    private static CareTask task(SourceType basis, TaskStatus status) {
        var task = mock(CareTask.class);
        when(task.id()).thenReturn(9L);
        when(task.residentId()).thenReturn(101L);
        when(task.basisType()).thenReturn(basis);
        when(task.basisId()).thenReturn(5L);
        when(task.shiftId()).thenReturn(40L);
        when(task.floorAreaId()).thenReturn(7L);
        when(task.importance()).thenReturn(Importance.MANDATORY);
        when(task.status()).thenReturn(status);
        return task;
    }

    @Test
    void FR_044_measurementTaskIsReportedOnceWithItsScheduleAndNoIndicators() {
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true, false);
        var task = task(SourceType.MEASUREMENT, TaskStatus.NOT_PERFORMED);

        notices.measurementMissed(task, SHIFT_END, "NOT_PERFORMED");
        notices.measurementMissed(task, SHIFT_END, "OVERDUE_AT_SHIFT_END");

        var event = ArgumentCaptor.forClass(MeasurementTaskMissed.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().taskId()).isEqualTo(9);
        assertThat(event.getValue().residentId()).isEqualTo(101);
        assertThat(event.getValue().measurementScheduleId()).isEqualTo(5);
        assertThat(event.getValue().vitalSignIds()).isEmpty();
        assertThat(event.getValue().reason()).isEqualTo("NOT_PERFORMED");
        assertThat(event.getValue().importance()).isEqualTo(Importance.MANDATORY);
        assertThat(event.getValue().context().occurredAt()).isEqualTo(SHIFT_END);
        assertThat(event.getValue().context().floorAreaId()).contains(7L);
    }

    @Test
    void FR_044_taskFromAnotherSourceIsNotReported() {
        notices.measurementMissed(
                task(SourceType.CARE_PLAN_ITEM, TaskStatus.NOT_PERFORMED), SHIFT_END, "NOT_PERFORMED");

        verify(events, never()).publishEvent(any(Object.class));
        verify(markers, never()).mark(anyString(), anyString(), anyLong(), anyString(), any());
    }

    @Test
    void FR_044_overdueTaskIsReportedOnlyFromTheEndOfItsShift() {
        var commands = mock(CareTaskService.class);
        var staff = mock(ShiftStaffPort.class);
        var spy = mock(CareTaskNotices.class);
        var task = task(SourceType.MEASUREMENT, TaskStatus.OVERDUE);
        when(task.carryover()).thenReturn("AWAITING_HANDOVER_CONFIRMATION");
        when(commands.locked(9L)).thenReturn(task);
        when(commands.shift(40L))
                .thenReturn(new ShiftStaffPort.ShiftView(40, 7, SHIFT_END.minusSeconds(8 * 3600), SHIFT_END, true));
        when(commands.staff()).thenReturn(staff);
        var processor = new CareTaskClockProcessor(
                mock(CareTaskStore.class),
                commands,
                spy,
                mock(ResidentApi.class),
                mock(ParameterApi.class),
                mock(CareLockStore.class),
                mock(CareCatalogStore.class));

        processor.advance(9L, SHIFT_END.minusSeconds(60));
        verify(spy, never()).measurementMissed(any(), any(), anyString());

        processor.advance(9L, SHIFT_END.plusSeconds(60));
        verify(spy).measurementMissed(eq(task), eq(SHIFT_END), eq("OVERDUE_AT_SHIFT_END"));
    }
}
