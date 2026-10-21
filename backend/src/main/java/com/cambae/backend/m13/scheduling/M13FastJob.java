package com.cambae.backend.m13.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m13.internal.job.service.M13Jobs;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job 20 giây của M13 ({@code ViecNhanhM13}; research B-10): chu kỳ để đạt SC-002, SC-003; là thông số vận hành, không
 * phải CFG. Profile test tắt lịch, test gọi {@link #run()}.
 */
@Component
public class M13FastJob implements ScheduledJob {

    private final M13Jobs jobs;

    public M13FastJob(M13Jobs jobs) {
        this.jobs = jobs;
    }

    @Scheduled(cron = "${eldercare.jobs.m13-fast.cron:*/20 * * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M13Jobs.FAST, lockAtMostFor = "PT5M")
    public void scheduled() {
        jobs.runFast();
    }

    /** Chạy một lượt, trả id lần chạy ({@code scheduler_run}). */
    public long run() {
        return jobs.runFast();
    }

    @Override
    public String name() {
        return M13Jobs.FAST;
    }

    @Override
    public void runNow() {
        run();
    }
}
