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
public class CareDailyJob implements ScheduledJob {
    private final List<CareDailyWork> work;
    private final CareDailyWorkRunner runner;
    private final SchedulerRuns runs;
    private final BusinessTime time;
    private final Environment environment;

    public CareDailyJob(
            List<CareDailyWork> work,
            CareDailyWorkRunner runner,
            SchedulerRuns runs,
            BusinessTime time,
            Environment environment) {
        this.work = List.copyOf(work);
        this.runner = runner;
        this.runs = runs;
        this.time = time;
        this.environment = environment;
    }

    @Scheduled(cron = "${eldercare.jobs.care-daily.cron:0 0 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "CARE_DAILY")
    public void scheduled() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = "CARE_DAILY")
    public void atStartup() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @Override
    public String name() {
        return "CARE_DAILY";
    }

    @Override
    public void runNow() {
        String runId = runs.start(name());
        int count = 0;
        var now = time.now();
        try {
            var today = java.time.LocalDate.ofInstant(now, BusinessTime.ZONE);
            for (var step : work)
                for (var date :
                        step.dueDates(today).stream().distinct().sorted().toList()) {
                    if (date.isAfter(today)) throw new IllegalArgumentException("Mốc công việc nằm trong tương lai");
                    count += runner.run(step, date, now);
                }
            runs.finish(runId, SchedulerRuns.Result.SUCCEEDED, count);
        } catch (RuntimeException error) {
            runs.finish(runId, SchedulerRuns.Result.FAILED, count);
            throw error;
        }
    }
}
