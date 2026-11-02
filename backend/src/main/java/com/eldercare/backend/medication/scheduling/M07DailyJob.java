package com.eldercare.backend.medication.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.schedule.service.M07JobService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily job for M07 (research D-19).
 * Runs at 00:00 (Asia/Ho_Chi_Minh) and upon application startup (catch-up run).
 */
@Component
public class M07DailyJob implements ScheduledJob {

    private final M07JobService jobService;
    private final BusinessTime businessTime;
    private final SchedulerRuns runs;
    private final boolean onStartup;

    public M07DailyJob(
            M07JobService jobService,
            BusinessTime businessTime,
            SchedulerRuns runs,
            @Value("${eldercare.jobs.m07-daily.on-startup:${eldercare.scheduling.enabled:true}}") boolean onStartup) {
        this.jobService = jobService;
        this.businessTime = businessTime;
        this.runs = runs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m07-daily.cron:0 0 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M07Jobs.DAILY, lockAtMostFor = "PT30M")
    public void scheduled() {
        runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = M07Jobs.DAILY, lockAtMostFor = "PT30M")
    public void atStartup() {
        if (onStartup) {
            runNow();
        }
    }

    @Override
    public String name() {
        return M07Jobs.DAILY;
    }

    @Override
    public void runNow() {
        String runId = runs.start(name());
        try {
            jobService.runDailyTasks(businessTime.today(), businessTime.now());
            runs.finish(runId, SchedulerRuns.Result.SUCCEEDED, 0);
        } catch (RuntimeException error) {
            runs.finish(runId, SchedulerRuns.Result.FAILED, 0);
            throw error;
        }
    }
}
