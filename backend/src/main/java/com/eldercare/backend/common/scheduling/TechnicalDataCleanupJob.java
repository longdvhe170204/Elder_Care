package com.eldercare.backend.common.scheduling;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job dọn dữ liệu kỹ thuật, 03:15 hằng ngày (audit cơ sở dữ liệu F-11, F-12). */
@Component
public class TechnicalDataCleanupJob implements ScheduledJob {

    private final TechnicalDataCleanup cleanup;

    public TechnicalDataCleanupJob(TechnicalDataCleanup cleanup) {
        this.cleanup = cleanup;
    }

    @Scheduled(cron = "${eldercare.jobs.clean-technical-data.cron:0 15 3 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = TechnicalDataCleanup.JOB)
    public void scheduled() {
        cleanup.run();
    }

    @Override
    public String name() {
        return TechnicalDataCleanup.JOB;
    }

    @Override
    public void runNow() {
        cleanup.run();
    }
}
