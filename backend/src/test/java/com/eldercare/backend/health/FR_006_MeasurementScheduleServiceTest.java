package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.care.api.TaskSourceApi;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.dto.MeasurementFrequency;
import com.eldercare.backend.health.api.events.MeasurementScheduleChanged;
import com.eldercare.backend.health.internal.governance.service.VitalSignLock;
import com.eldercare.backend.health.internal.schedule.domain.MeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.NewMeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.ScheduleFrequency;
import com.eldercare.backend.health.internal.schedule.persistence.MeasurementScheduleStore;
import com.eldercare.backend.health.internal.schedule.service.MeasurementScheduleService;
import com.eldercare.backend.health.internal.threshold.service.ThresholdIndicators;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/** Vòng đời lịch đo (bảng FR-006; FR-003 → FR-005; research S-05; tasks T020, T023). */
class FR_006_MeasurementScheduleServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");
    private static final long A = 101L;

    private final MeasurementScheduleStore store = mock(MeasurementScheduleStore.class);
    private final ThresholdIndicators indicators = mock(ThresholdIndicators.class);
    private final ResidentApi residents = mock(ResidentApi.class);
    private final VitalSignLock lock = mock(VitalSignLock.class);
    private final TaskSourceApi taskSources = mock(TaskSourceApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final MeasurementScheduleService service =
            new MeasurementScheduleService(store, indicators, residents, lock, taskSources, audit, events, time);
    private final ScheduleFrequency twiceDaily = new ScheduleFrequency(
            MeasurementFrequency.CLOCK_TIMES, List.of(LocalTime.of(6, 0), LocalTime.of(18, 0)), null, null, Set.of());

    @BeforeEach
    void defaults() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(store.insert(any(), anyLong(), any())).thenReturn(60L);
    }

    private NewMeasurementSchedule schedule(Instant start) {
        return new NewMeasurementSchedule(Set.of(3L, 4L), twiceDaily, start, null, "MANDATORY", "Theo dõi huyết áp");
    }

    private MeasurementSchedule existing(long id, String status, Instant start) {
        return new MeasurementSchedule(id, A, schedule(start), status, null, null, null, 2);
    }

    @Test
    void FR_006_createEffectiveWhenStartNotAfterCommandAndPendingOtherwise() {
        service.create(A, schedule(NOW.minusSeconds(60)), user);
        service.create(A, schedule(NOW.plus(Duration.ofDays(1))), user);

        var inserted = ArgumentCaptor.forClass(MeasurementSchedule.class);
        verify(store, org.mockito.Mockito.times(2)).insert(inserted.capture(), anyLong(), any());
        assertThat(inserted.getAllValues())
                .extracting(MeasurementSchedule::status)
                .containsExactly("EFFECTIVE", "PENDING");
        verify(lock, org.mockito.Mockito.times(2)).lock(A);
        verify(indicators, org.mockito.Mockito.times(2)).require(3L);
        verify(indicators, org.mockito.Mockito.atLeastOnce()).require(4L);
    }

    @Test
    void FR_006_createBlockedForFinalStatusAndInvalidContent() {
        when(residents.isFinalStatus(A)).thenReturn(true);
        assertThatThrownBy(() -> service.create(A, schedule(NOW), user)).isInstanceOf(BusinessException.class);

        when(residents.isFinalStatus(A)).thenReturn(false);
        var noIndicators = new NewMeasurementSchedule(Set.of(), twiceDaily, NOW, null, "MANDATORY", null);
        assertThatThrownBy(() -> service.create(A, noIndicators, user)).isInstanceOf(BusinessException.class);
        var endBeforeStart =
                new NewMeasurementSchedule(Set.of(3L), twiceDaily, NOW, NOW.minusSeconds(1), "MANDATORY", null);
        assertThatThrownBy(() -> service.create(A, endBeforeStart, user)).isInstanceOf(BusinessException.class);
        verify(store, never()).insert(any(), anyLong(), any());
    }

    @Test
    void FR_005_changeCreatesReplacementAndStopsOldAtItsEffectiveTimeInSameCommand() {
        when(store.lock(50)).thenReturn(Optional.of(existing(50, "EFFECTIVE", NOW.minus(Duration.ofDays(3)))));
        var tomorrow = NOW.plus(Duration.ofDays(1));

        long id = service.change(50, schedule(tomorrow), new Reason("Đổi giờ đo"), user);

        assertThat(id).isEqualTo(60L);
        var inserted = ArgumentCaptor.forClass(MeasurementSchedule.class);
        verify(store).insert(inserted.capture(), anyLong(), any());
        assertThat(inserted.getValue().replaces()).isEqualTo(50L);
        assertThat(inserted.getValue().status()).isEqualTo("PENDING");
        verify(store).stop(50, tomorrow, "Đổi giờ đo", 900L, 2);
        verify(taskSources).sourceChanged(SourceType.MEASUREMENT, 50L);
        verify(events).publishEvent(any(MeasurementScheduleChanged.class));
    }

    @Test
    void FR_005_changeFromPastStartStopsOldAtCommandTime() {
        when(store.lock(50)).thenReturn(Optional.of(existing(50, "EFFECTIVE", NOW.minus(Duration.ofDays(3)))));

        service.change(50, schedule(NOW.minus(Duration.ofHours(2))), new Reason("Đổi giờ đo"), user);

        verify(store).stop(50, NOW, "Đổi giờ đo", 900L, 2);
    }
}
