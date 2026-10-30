package com.eldercare.backend.common.file;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job dọn tệp chưa gắn, mỗi giờ (001 T004; research H-13). */
@Component
public class UnattachedFileCleanupJob implements ScheduledJob {

    private final UnattachedFileCleanup cleanup;

    public UnattachedFileCleanupJob(UnattachedFileCleanup cleanup) {
        this.cleanup = cleanup;
    }

    @Scheduled(cron = "${eldercare.jobs.clean-unattached-files.cron:0 30 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = UnattachedFileCleanup.JOB)
    public void scheduled() {
        cleanup.run();
    }

    @Override
    public String name() {
        return UnattachedFileCleanup.JOB;
    }

    @Override
    public void runNow() {
        cleanup.run();
    }
}
