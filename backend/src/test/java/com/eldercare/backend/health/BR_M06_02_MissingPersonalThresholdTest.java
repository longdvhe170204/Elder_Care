package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.health.api.events.MissingPersonalThresholdReminder;
import com.eldercare.backend.health.internal.governance.domain.ResidentVitalSignState;
import com.eldercare.backend.health.internal.governance.service.VitalSignLock;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdVersion;
import com.eldercare.backend.health.internal.threshold.persistence.ThresholdVersionStore;
import com.eldercare.backend.health.internal.threshold.service.MissingThresholdReminders;
import com.eldercare.backend.health.internal.threshold.service.ThresholdFinalStatusListener;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusPeriod;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/** Nhắc thiếu ngưỡng cá nhân (FR-025, BR-M06-02, Q-330) và kết thúc ngưỡng cá nhân khi trạng thái cuối (FR-022). */
class BR_M06_02_MissingPersonalThresholdTest {
    private static final Instant NOW = Instant.parse("2026-11-04T17:15:00Z");
    private static final long A = 101L;

    private final ResidentApi residents = mock(ResidentApi.class);
    private final ResidentStatusApi statuses = mock(ResidentStatusApi.class);
    private final ThresholdVersionStore store = mock(ThresholdVersionStore.class);
    private final ResidentModuleStateApi states = mock(ResidentModuleStateApi.class);
    private final VitalSignLock lock = mock(VitalSignLock.class);
    private final ParameterApi parameters = mock(ParameterApi.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final MissingThresholdReminders reminders =
            new MissingThresholdReminders(residents, statuses, store, states, lock, parameters, events);

    @BeforeEach
    void defaults() {
        var days = mock(ParameterValue.class);
        when(days.asDuration()).thenReturn(Duration.ofDays(7));
        when(parameters.read(ParameterCode.CFG_M06_01)).thenReturn(days);
        when(residents.byStatus(Set.of(ResidentStatus.IN_RESIDENCE))).thenReturn(List.of(A));
        admittedDaysAgo(8);
        when(store.hasPersonalOpen(A)).thenReturn(false);
        when(states.get(ResidentVitalSignState.KIND, A)).thenReturn(Optional.empty());
        when(states.lockForUpdate(ResidentVitalSignState.KIND, A)).thenReturn(Optional.empty());
    }

    private void admittedDaysAgo(int days) {
        when(statuses.statusPeriods(eq(A), eq(Set.of(ResidentStatus.IN_RESIDENCE)), any(), any()))
                .thenReturn(List.of(new StatusPeriod(
                        ResidentStatus.IN_RESIDENCE, NOW.minus(Duration.ofDays(days)), Optional.empty())));
    }

    @Test
    void BR_M06_02_residentPastThresholdWithoutPersonalThresholdIsRemindedOnce() {
        assertThat(reminders.remind(NOW)).isEqualTo(1);

        verify(lock).lock(A);
        verify(states).save(ResidentVitalSignState.KIND, A, new ResidentVitalSignState(NOW));
        verify(events).publishEvent(any(MissingPersonalThresholdReminder.class));
        assertThat(reminders.missing(NOW)).containsExactly(A);
    }

    @Test
    void BR_M06_02_alreadyRemindedOrTooRecentOrHasPersonalIsSkipped() {
        when(states.get(ResidentVitalSignState.KIND, A))
                .thenReturn(Optional.of(new ResidentVitalSignState(NOW.minus(Duration.ofDays(1)))));
        assertThat(reminders.remind(NOW)).isZero();
        assertThat(reminders.missing(NOW)).containsExactly(A);

        when(states.get(ResidentVitalSignState.KIND, A)).thenReturn(Optional.empty());
        admittedDaysAgo(5);
        assertThat(reminders.remind(NOW)).isZero();
        assertThat(reminders.missing(NOW)).isEmpty();

        admittedDaysAgo(8);
        when(store.hasPersonalOpen(A)).thenReturn(true);
        assertThat(reminders.remind(NOW)).isZero();
        assertThat(reminders.missing(NOW)).isEmpty();
        verify(events, never()).publishEvent(any());
    }

    @Test
    void Q_330_stoppedLastPersonalThresholdAllowsNewReminder() {
        when(store.hasPersonalOpen(A)).thenReturn(false);

        reminders.allowReminderAgain(A);

        verify(lock).lock(A);
        verify(states).save(ResidentVitalSignState.KIND, A, new ResidentVitalSignState(null));
    }

    @Test
    void FR_022_finalStatusEndsPersonalThresholds() {
        var effective = new ThresholdVersion(
                11, "PERSONAL", A, 3, NOW.minus(Duration.ofDays(9)), null, "EFFECTIVE", Map.of(), 1);
        var pending =
                new ThresholdVersion(12, "PERSONAL", A, 3, NOW.plus(Duration.ofDays(1)), null, "PENDING", Map.of(), 0);
        when(store.personalOpen(A)).thenReturn(List.of(effective, pending));
        var listener = new ThresholdFinalStatusListener(store, lock);
        var deceased = NOW.minusSeconds(60);

        listener.on(
                new ResidentStatusChanged(A, ResidentStatus.HOSPITALIZED, ResidentStatus.DECEASED, null, 1L, deceased));

        verify(store).end(11, "EXPIRED", deceased, null, null, 1);
        verify(store).end(12, "CANCELLED", null, null, null, 0);

        listener.on(
                new ResidentStatusChanged(A, ResidentStatus.IN_RESIDENCE, ResidentStatus.HOSPITALIZED, null, 2L, NOW));
        verify(store, org.mockito.Mockito.times(2)).end(anyLong(), any(), any(), any(), any(), anyInt());
    }
}
