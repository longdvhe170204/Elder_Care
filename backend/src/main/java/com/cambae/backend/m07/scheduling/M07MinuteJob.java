package com.cambae.backend.m07.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m07.internal.service.M07JobService;
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

    public M07MinuteJob(M07JobService jobService, BusinessTime businessTime) {
        this.jobService = jobService;
        this.businessTime = businessTime;
    }

    @Scheduled(cron = "${eldercare.jobs.m07-minute.cron:0 * * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M07Jobs.MINUTE, lockAtMostFor = "PT5M")
    public void scheduled() {
        jobService.runMinuteTasks(businessTime.now());
    }

    @Override
    public String name() {
        return M07Jobs.MINUTE;
    }

    @Override
    public void runNow() {
        jobService.runMinuteTasks(businessTime.now());
    }
}
