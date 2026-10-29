package com.eldercare.backend.staffing.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.staffing.internal.service.M09Jobs;
import com.eldercare.backend.staffing.internal.service.M09TimedTask.Cadence;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** T008: khung job HOURLY, giờ Việt Nam; tác vụ nghiệp vụ đăng ký ở story tương ứng. */
@Component
public class M09HourlyJob implements ScheduledJob {
    private final M09Jobs jobs;
    private final boolean onStartup;

    public M09HourlyJob(M09Jobs jobs, @Value("${eldercare.jobs.m09-hourly.on-startup:true}") boolean onStartup) {
        this.jobs = jobs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m09-hourly.cron:0 0 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "M09_HOURLY")
    public void scheduled() {
        jobs.runDue(Cadence.HOURLY);
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = "M09_HOURLY")
    public void atStartup() {
        if (onStartup) jobs.runDue(Cadence.HOURLY);
    }

    public String name() {
        return "M09_HOURLY";
    }

    public void runNow() {
        jobs.runDue(Cadence.HOURLY);
    }
}
