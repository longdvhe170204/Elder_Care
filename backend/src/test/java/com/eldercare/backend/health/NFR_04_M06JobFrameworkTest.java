package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.internal.scheduling.persistence.M06JobRunStore;
import com.eldercare.backend.health.internal.scheduling.service.M06Jobs;
import com.eldercare.backend.health.internal.scheduling.service.M06TimedTask;
import com.eldercare.backend.health.internal.scheduling.service.M06TimedTask.Cadence;
import com.eldercare.backend.health.scheduling.M06DailyJob;
import com.eldercare.backend.health.scheduling.M06MinuteJob;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class NFR_04_M06JobFrameworkTest {

    private static final Instant NOW =
            OffsetDateTime.parse("2030-03-04T08:21:45+07:00").toInstant();

    @Test
    void NFR_04_minuteRunInvokesEachTaskOnceAtTheCurrentScheduledMinute() {
        var minute = task("MINUTE_TASK", Cadence.MINUTE, 2);
        var daily = task("DAILY_TASK", Cadence.DAILY, 9);
        var runs = mock(SchedulerRuns.class);
        var store = mock(M06JobRunStore.class);
        when(runs.start(M06Jobs.MINUTE)).thenReturn("run-1");
        var jobs = new M06Jobs(List.of(minute, daily), runs, store, timeAt(NOW));

        jobs.runMinute();

        verify(minute).run(Instant.parse("2030-03-04T01:21:00Z"), NOW);
        verify(daily, never()).run(any(), any());
        verify(runs).finish("run-1", SchedulerRuns.Result.SUCCEEDED, 2);
    }

    @Test
    void NFR_04_dailyRunCatchesUpOneDueDayAndMarksItOnce() {
        var daily = task("DAILY_TASK", Cadence.DAILY, 1);
        var runs = mock(SchedulerRuns.class);
        var store = mock(M06JobRunStore.class);
        when(runs.start(M06Jobs.DAILY)).thenReturn("run-2");
        when(store.completed(M06Jobs.DAILY, LocalDate.of(2030, 3, 4))).thenReturn(false, true);
        var jobs = new M06Jobs(List.of(daily), runs, store, timeAt(NOW));

        jobs.runDailyIfDue();
        jobs.runDailyIfDue();

        verify(daily).run(Instant.parse("2030-03-03T17:15:00Z"), NOW);
        verify(store).markCompleted(M06Jobs.DAILY, LocalDate.of(2030, 3, 4), "run-2");
        verify(runs).finish("run-2", SchedulerRuns.Result.SUCCEEDED, 1);
    }

    @Test
    void NFR_04_failedDailyTaskDoesNotMarkTheDayComplete() {
        var failing = task("FAILING", Cadence.DAILY, 0);
        var succeeding = task("SUCCEEDING", Cadence.DAILY, 3);
        when(failing.run(any(), any())).thenThrow(new IllegalStateException("failed"));
        var runs = mock(SchedulerRuns.class);
        var store = mock(M06JobRunStore.class);
        when(runs.start(M06Jobs.DAILY)).thenReturn("run-3");
        var jobs = new M06Jobs(List.of(failing, succeeding), runs, store, timeAt(NOW));

        jobs.runDailyIfDue();

        verify(succeeding).run(any(), eq(NOW));
        verify(store, never()).markCompleted(anyString(), any(), anyString());
        verify(runs).finish("run-3", SchedulerRuns.Result.FAILED, 3);
    }

    @Test
    void NFR_04_schedulingWrappersRunNowAndHonorStartupSwitch() {
        var jobs = mock(M06Jobs.class);
        var minute = new M06MinuteJob(jobs, false);
        var daily = new M06DailyJob(jobs, true);

        minute.runNow();
        minute.atStartup();
        daily.atStartup();

        assertThat(minute.name()).isEqualTo(M06Jobs.MINUTE);
        assertThat(daily.name()).isEqualTo(M06Jobs.DAILY);
        verify(jobs).runMinute();
        verify(jobs).runDailyIfDue();
    }

    private static M06TimedTask task(String name, Cadence cadence, int processed) {
        var task = mock(M06TimedTask.class);
        when(task.name()).thenReturn(name);
        when(task.cadence()).thenReturn(cadence);
        when(task.run(any(), any())).thenReturn(processed);
        return task;
    }

    private static BusinessTime timeAt(Instant instant) {
        return new BusinessTime(Clock.fixed(instant, BusinessTime.ZONE));
    }
}
