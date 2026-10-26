package com.cambae.backend.m03.scheduling;

import com.cambae.backend.common.scheduling.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.internal.allocation.persistence.AllocationReminderStore;
import com.cambae.backend.m03.internal.allocation.service.AllocationReminderProcessor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** T059: chỉ nhắc theo đồng hồ, không lập phiếu nghiệp vụ. */
@Component
public class AllocationReminderJob implements ScheduledJob {
    private final AllocationReminderStore store;
    private final AllocationReminderProcessor processor;
    private final BusinessTime time;
    private final SchedulerRuns runs;
    private final Environment environment;
    private final boolean startup;

    public AllocationReminderJob(
            AllocationReminderStore store,
            AllocationReminderProcessor processor,
            BusinessTime time,
            SchedulerRuns runs,
            Environment environment,
            @Value("${eldercare.jobs.allocation-reminder.on-startup:${eldercare.scheduling.enabled:true}}")
                    boolean startup) {
        this.store = store;
        this.processor = processor;
        this.time = time;
        this.runs = runs;
        this.environment = environment;
        this.startup = startup;
    }

    @Scheduled(cron = "${eldercare.jobs.allocation-reminder.cron:0 */15 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "REMIND_ALLOCATIONS")
    public void scheduled() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = "REMIND_ALLOCATIONS")
    public void atStartup() {
        if (startup && !environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @Override
    public String name() {
        return "REMIND_ALLOCATIONS";
    }

    @Override
    public void runNow() {
        long run = runs.start(name());
        int count = 0;
        try {
            for (var row : store.candidates()) if (processor.remind(row, run)) count++;
            runs.finish(run, SchedulerRuns.Result.SUCCEEDED, count);
        } catch (RuntimeException error) {
            runs.finish(run, SchedulerRuns.Result.FAILED, count);
            throw error;
        }
    }
}
