package com.eldercare.backend.nutrition.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Việc hằng ngày của M08 lúc 00:00 (Asia/Ho_Chi_Minh) và một lần khi khởi động để chạy bù (research M-15). */
@Component
public class M08DailyJob implements ScheduledJob {

    private final M08JobService jobService;
    private final BusinessTime businessTime;
    private final SchedulerRuns runs;
    private final boolean onStartup;

    public M08DailyJob(
            M08JobService jobService,
            BusinessTime businessTime,
            SchedulerRuns runs,
            @Value("${eldercare.jobs.m08-daily.on-startup:${eldercare.scheduling.enabled:true}}") boolean onStartup) {
        this.jobService = jobService;
        this.businessTime = businessTime;
        this.runs = runs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m08-daily.cron:0 0 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M08Jobs.DAILY, lockAtMostFor = "PT30M")
    public void scheduled() {
        runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = M08Jobs.DAILY, lockAtMostFor = "PT30M")
    public void atStartup() {
        if (onStartup) {
            runNow();
        }
    }

    @Override
    public String name() {
        return M08Jobs.DAILY;
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
