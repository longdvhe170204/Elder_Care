package com.eldercare.backend.nutrition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import com.eldercare.backend.nutrition.scheduling.M08DailyJob;
import com.eldercare.backend.nutrition.scheduling.M08MinuteJob;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class M08JobSchedulingTest {

    @Test
    void NFR_04_minuteJobWritesSchedulerRun() {
        M08JobService service = mock(M08JobService.class);
        BusinessTime time = mock(BusinessTime.class);
        SchedulerRuns runs = mock(SchedulerRuns.class);
        Instant now = Instant.parse("2026-11-10T01:00:00Z");
        when(time.now()).thenReturn(now);
        when(runs.start("M08_MINUTE")).thenReturn("run-011");

        new M08MinuteJob(service, time, runs, false).runNow();

        verify(service).runMinuteTasks(now);
        verify(runs).finish("run-011", SchedulerRuns.Result.SUCCEEDED, 0);
    }

    @Test
    void NFR_04_dailyJobWritesSchedulerRun() {
        M08JobService service = mock(M08JobService.class);
        BusinessTime time = mock(BusinessTime.class);
        SchedulerRuns runs = mock(SchedulerRuns.class);
        Instant now = Instant.parse("2026-11-09T17:00:00Z");
        LocalDate today = LocalDate.parse("2026-11-10");
        when(time.now()).thenReturn(now);
        when(time.today()).thenReturn(today);
        when(runs.start("M08_DAILY")).thenReturn("run-012");

        new M08DailyJob(service, time, runs, false).runNow();

        verify(service).runDailyTasks(today, now);
        verify(runs).finish("run-012", SchedulerRuns.Result.SUCCEEDED, 0);
    }
}
