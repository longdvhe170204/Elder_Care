package com.eldercare.backend.family.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.family.internal.scheduling.service.M10Jobs;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask.Cadence;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Việc hằng giờ của M10 ({@code ViecHangGioM10}; research F-17), giờ Việt Nam, và lần chạy lúc khởi động. */
@Component
public class M10HourlyJob implements ScheduledJob {

    private static final String NAME = "M10_HOURLY";

    private final M10Jobs jobs;
    private final boolean onStartup;

    public M10HourlyJob(M10Jobs jobs, @Value("${eldercare.jobs.m10-hourly.on-startup:true}") boolean onStartup) {
        this.jobs = jobs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m10-hourly.cron:0 0 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = NAME)
    public void scheduled() {
        jobs.runDue(Cadence.HOURLY);
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = NAME)
    public void atStartup() {
        if (onStartup) {
            jobs.runDue(Cadence.HOURLY);
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void runNow() {
        jobs.runDue(Cadence.HOURLY);
    }
}
