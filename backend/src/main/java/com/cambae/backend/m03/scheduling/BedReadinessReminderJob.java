package com.cambae.backend.m03.scheduling;

import com.cambae.backend.common.scheduling.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.internal.bed.persistence.BedReadinessStore;
import com.cambae.backend.m03.internal.bed.service.BedReadinessReminderProcessor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** T059: chỉ nhắc theo đồng hồ, không lập phiếu nghiệp vụ. */
@Component
public class BedReadinessReminderJob implements ScheduledJob {
    private final BedReadinessStore store;
    private final BedReadinessReminderProcessor processor;
    private final BusinessTime time;
    private final SchedulerRuns runs;
    private final Environment environment;
    private final boolean startup;

    public BedReadinessReminderJob(
            BedReadinessStore store,
            BedReadinessReminderProcessor processor,
            BusinessTime time,
            SchedulerRuns runs,
            Environment environment,
            @Value("${eldercare.jobs.bed-readiness-reminder.on-startup:true}") boolean startup) {
        this.store = store;
        this.processor = processor;
        this.time = time;
        this.runs = runs;
        this.environment = environment;
        this.startup = startup;
    }

    @Scheduled(cron = "${eldercare.jobs.bed-readiness-reminder.cron:0 */15 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "REMIND_BED_READINESS")
    public void scheduled() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = "REMIND_BED_READINESS")
    public void atStartup() {
        if (startup && !environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @Override
    public String name() {
        return "REMIND_BED_READINESS";
    }

    @Override
    public void runNow() {
        long run = runs.start(name());
        int count = 0;
        try {
            for (var row : store.due(time.now())) if (processor.remind(row.bedId(), run)) count++;
            runs.finish(run, SchedulerRuns.Result.SUCCEEDED, count);
        } catch (RuntimeException error) {
            runs.finish(run, SchedulerRuns.Result.FAILED, count);
            throw error;
        }
    }
}
