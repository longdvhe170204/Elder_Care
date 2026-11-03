package com.eldercare.backend.health.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.health.internal.scheduling.service.M06Jobs;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Việc hằng ngày M06 lúc 00:15, chạy bù ngày gần nhất chưa hoàn tất. */
@Component
public class M06DailyJob implements ScheduledJob {
    private final M06Jobs jobs;
    private final boolean onStartup;

    public M06DailyJob(
            M06Jobs jobs,
            @Value("${eldercare.jobs.m06-daily.on-startup:${eldercare.scheduling.enabled:true}}") boolean onStartup) {
        this.jobs = jobs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m06-daily.cron:0 15 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M06Jobs.DAILY)
    public void scheduled() {
        jobs.runDailyIfDue();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = M06Jobs.DAILY)
    public void atStartup() {
        if (onStartup) jobs.runDailyIfDue();
    }

    @Override
    public String name() {
        return M06Jobs.DAILY;
    }

    @Override
    public void runNow() {
        jobs.runDailyIfDue();
    }
}
