package com.eldercare.backend.care.scheduling;

import com.eldercare.backend.care.internal.scheduling.service.*;
import com.eldercare.backend.common.scheduling.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** T006/C-12: khung đăng ký việc, chạy bù lúc khởi động; không có lệnh lập công việc. */
@Component
public class CareMinuteJob implements ScheduledJob {
    private final List<CareMinuteWork> work;

    private final SchedulerRuns runs;
    private final BusinessTime time;
    private final Environment environment;

    public CareMinuteJob(List<CareMinuteWork> work, SchedulerRuns runs, BusinessTime time, Environment environment) {
        this.work = List.copyOf(work);
        this.runs = runs;
        this.time = time;
        this.environment = environment;
    }

    @Scheduled(cron = "${eldercare.jobs.care-minute.cron:0 * * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "CARE_MINUTE")
    public void scheduled() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = "CARE_MINUTE")
    public void atStartup() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @Override
    public String name() {
        return "CARE_MINUTE";
    }

    @Override
    public void runNow() {
        String runId = runs.start(name());
        int count = 0;
        var now = time.now();
        try {
            for (var step : work) count += step.run(now);
            runs.finish(runId, SchedulerRuns.Result.SUCCEEDED, count);
        } catch (RuntimeException error) {
            runs.finish(runId, SchedulerRuns.Result.FAILED, count);
            throw error;
        }
    }
}
