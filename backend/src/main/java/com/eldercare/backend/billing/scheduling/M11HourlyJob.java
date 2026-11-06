package com.eldercare.backend.billing.scheduling;

import com.eldercare.backend.billing.internal.period.domain.CostJobCadence;
import com.eldercare.backend.billing.internal.period.service.M11Jobs;
import com.eldercare.backend.common.scheduling.ScheduledJob;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** T007: khung HOURLY, nghiệp vụ đăng ký ở các story; không tự lập khoản hoặc bảng. */
@Component
public class M11HourlyJob implements ScheduledJob {
    private final M11Jobs jobs;
    private final boolean onStartup;

    public M11HourlyJob(
            M11Jobs jobs,
            @Value("${eldercare.jobs.m11-hourly.on-startup:${eldercare.scheduling.enabled:true}}") boolean onStartup) {
        this.jobs = jobs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m11-hourly.cron:0 0 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "M11_HOURLY")
    public void scheduled() {
        jobs.runDue(CostJobCadence.HOURLY);
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = "M11_HOURLY")
    public void atStartup() {
        if (onStartup) jobs.runDue(CostJobCadence.HOURLY);
    }

    public String name() {
        return "M11_HOURLY";
    }

    public void runNow() {
        jobs.runDue(CostJobCadence.HOURLY);
    }
}
