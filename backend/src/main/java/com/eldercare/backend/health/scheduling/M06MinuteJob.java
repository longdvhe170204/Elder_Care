package com.eldercare.backend.health.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.health.internal.scheduling.service.M06Jobs;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Việc theo phút M06, gồm chạy bù một cấp khi ứng dụng khởi động. */
@Component
public class M06MinuteJob implements ScheduledJob {
    private final M06Jobs jobs;
    private final boolean onStartup;

    public M06MinuteJob(
            M06Jobs jobs,
            @Value("${eldercare.jobs.m06-minute.on-startup:${eldercare.scheduling.enabled:true}}") boolean onStartup) {
        this.jobs = jobs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m06-minute.cron:0 * * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M06Jobs.MINUTE)
    public void scheduled() {
        jobs.runMinute();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = M06Jobs.MINUTE)
    public void atStartup() {
        if (onStartup) jobs.runMinute();
    }

    @Override
    public String name() {
        return M06Jobs.MINUTE;
    }

    @Override
    public void runNow() {
        jobs.runMinute();
    }
}
