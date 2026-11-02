package com.eldercare.backend.medication;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.schedule.service.M07JobService;
import com.eldercare.backend.medication.scheduling.M07MinuteJob;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class M07JobSchedulingTest {

    @Test
    void R_15_minuteJobWritesStructuredSchedulerRun() {
        M07JobService service = mock(M07JobService.class);
        BusinessTime time = mock(BusinessTime.class);
        SchedulerRuns runs = mock(SchedulerRuns.class);
        Instant now = Instant.parse("2026-10-30T08:00:00Z");
        when(time.now()).thenReturn(now);
        when(runs.start("M07_MINUTE")).thenReturn("run-006");

        new M07MinuteJob(service, time, runs).runNow();

        verify(service).runMinuteTasks(now);
        verify(runs).finish("run-006", SchedulerRuns.Result.SUCCEEDED, 0);
    }
}
