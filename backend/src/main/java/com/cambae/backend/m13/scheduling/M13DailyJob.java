package com.cambae.backend.m13.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m13.internal.job.service.M13Jobs;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job hằng ngày 07:30 của M13 ({@code ViecHangNgayM13}; research B-10) và lần chạy bù lúc khởi động (NFR-04). Profile
 * test tắt lịch và lần chạy lúc khởi động, test gọi {@link #catchUp()}.
 */
@Component
public class M13DailyJob implements ScheduledJob {

    private final M13Jobs jobs;
    private final boolean onStartup;

    public M13DailyJob(M13Jobs jobs, @Value("${eldercare.jobs.m13-daily.on-startup:true}") boolean onStartup) {
        this.jobs = jobs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m13-daily.cron:0 30 7 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M13Jobs.DAILY)
    public void scheduled() {
        jobs.runDailyIfDue();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = M13Jobs.DAILY)
    public void atStartup() {
        if (onStartup) {
            jobs.runDailyIfDue();
        }
    }

    /** Chạy cho hôm nay nếu đã tới giờ và chưa chạy. */
    public void catchUp() {
        jobs.runDailyIfDue();
    }

    @Override
    public String name() {
        return M13Jobs.DAILY;
    }

    @Override
    public void runNow() {
        catchUp();
    }
}
