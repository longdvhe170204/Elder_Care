package com.eldercare.backend.incident.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.incident.internal.scheduling.service.M05Jobs;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Việc theo phút M05, gồm chạy bù một cấp khi ứng dụng khởi động. */
@Component
public class M05MinuteJob implements ScheduledJob {
    private final M05Jobs jobs;
    private final boolean onStartup;

    public M05MinuteJob(
            M05Jobs jobs,
            @Value("${eldercare.jobs.m05-minute.on-startup:${eldercare.scheduling.enabled:true}}") boolean onStartup) {
        this.jobs = jobs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m05-minute.cron:0 * * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M05Jobs.MINUTE)
    public void scheduled() {
        jobs.runMinute();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = M05Jobs.MINUTE)
    public void atStartup() {
        if (onStartup) jobs.runMinute();
    }

    @Override
    public String name() {
        return M05Jobs.MINUTE;
    }

    @Override
    public void runNow() {
        jobs.runMinute();
    }
}
