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

/**
 * Việc theo phút của M08 (research M-15): chạy mỗi phút có khóa ShedLock và một lần khi khởi động để chạy bù. Job không
 * chốt suất, không lập phiếu (Q-306).
 */
@Component
public class M08MinuteJob implements ScheduledJob {

    private final M08JobService jobService;
    private final BusinessTime businessTime;
    private final SchedulerRuns runs;
    private final boolean onStartup;

    public M08MinuteJob(
            M08JobService jobService,
            BusinessTime businessTime,
            SchedulerRuns runs,
            @Value("${eldercare.jobs.m08-minute.on-startup:${eldercare.scheduling.enabled:true}}") boolean onStartup) {
        this.jobService = jobService;
        this.businessTime = businessTime;
        this.runs = runs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m08-minute.cron:0 * * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M08Jobs.MINUTE, lockAtMostFor = "PT5M")
    public void scheduled() {
        runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = M08Jobs.MINUTE, lockAtMostFor = "PT5M")
    public void atStartup() {
        if (onStartup) {
            runNow();
        }
    }

    @Override
    public String name() {
        return M08Jobs.MINUTE;
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
