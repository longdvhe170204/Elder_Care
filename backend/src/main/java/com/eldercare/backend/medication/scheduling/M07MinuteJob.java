package com.eldercare.backend.medication.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.schedule.service.M07JobService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Minute job for M07 (research D-19).
 * Runs every minute with ShedLock. Test profile disables scheduler; tests may invoke {@link #runNow()}.
 */
@Component
public class M07MinuteJob implements ScheduledJob {

    private final M07JobService jobService;
    private final BusinessTime businessTime;
    private final SchedulerRuns runs;

    public M07MinuteJob(M07JobService jobService, BusinessTime businessTime, SchedulerRuns runs) {
        this.jobService = jobService;
        this.businessTime = businessTime;
        this.runs = runs;
    }

    @Scheduled(cron = "${eldercare.jobs.m07-minute.cron:0 * * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M07Jobs.MINUTE, lockAtMostFor = "PT5M")
    public void scheduled() {
        runNow();
    }

    @Override
    public String name() {
        return M07Jobs.MINUTE;
    }

    @Override
    public void runNow() {
        String runId = runs.start(name());
        try {
            jobService.runMinuteTasks(businessTime.now());
            runs.finish(runId, SchedulerRuns.Result.SUCCEEDED, 0);
        } catch (RuntimeException error) {
            runs.finish(runId, SchedulerRuns.Result.FAILED, 0);
            throw error;
        }
    }
}
