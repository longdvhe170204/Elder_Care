package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.scheduling.service.M05Jobs;
import com.eldercare.backend.incident.internal.scheduling.service.M05MinuteTask;
import com.eldercare.backend.incident.scheduling.M05MinuteJob;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class NFR_04_M05JobFrameworkTest {

    @Test
    void NFR_04_minuteRunProcessesEveryTaskOnceAndRecordsPartialFailure() {
        Instant now = OffsetDateTime.parse("2030-03-04T08:21:45+07:00").toInstant();
        var failing = task("FAILING", 0);
        var succeeding = task("SUCCEEDING", 4);
        when(failing.run(any(), any())).thenThrow(new IllegalStateException("failed"));
        var runs = mock(SchedulerRuns.class);
        when(runs.start(M05Jobs.MINUTE)).thenReturn("run-1");
        var jobs =
                new M05Jobs(List.of(failing, succeeding), runs, new BusinessTime(Clock.fixed(now, BusinessTime.ZONE)));

        jobs.runMinute();

        verify(failing).run(Instant.parse("2030-03-04T01:21:00Z"), now);
        verify(succeeding).run(Instant.parse("2030-03-04T01:21:00Z"), now);
        verify(runs).finish("run-1", SchedulerRuns.Result.FAILED, 4);
    }

    @Test
    void NFR_04_schedulingWrapperRunsNowAndHonorsStartupSwitch() {
        var jobs = mock(M05Jobs.class);
        var disabled = new M05MinuteJob(jobs, false);
        var enabled = new M05MinuteJob(jobs, true);

        disabled.runNow();
        disabled.atStartup();
        enabled.atStartup();

        assertThat(disabled.name()).isEqualTo(M05Jobs.MINUTE);
        verify(jobs, times(2)).runMinute();
    }

    private static M05MinuteTask task(String name, int processed) {
        var task = mock(M05MinuteTask.class);
        when(task.name()).thenReturn(name);
        when(task.run(any(), any())).thenReturn(processed);
        return task;
    }
}
