package com.eldercare.backend.billing.scheduling;

import com.eldercare.backend.billing.internal.procurement.service.ProcurementReminders;
import com.eldercare.backend.common.scheduling.*;
import com.eldercare.backend.common.time.BusinessTime;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 019 T003 phần M11 (research W-09 {@code ViecHangNgayM11MuaSam} 07:00; BR-M11-20): chỉ nhắc, chạy bù lúc khởi động;
 * không lập yêu cầu (Q-306, {@code AssetArchTest}).
 */
@Component
public class ProcurementReminderJob implements ScheduledJob {
    static final String NAME = "REMIND_PROCUREMENT_REQUESTS";

    private final ProcurementReminders reminders;
    private final BusinessTime time;
    private final SchedulerRuns runs;
    private final Environment environment;
    private final boolean startup;

    public ProcurementReminderJob(
            ProcurementReminders reminders,
            BusinessTime time,
            SchedulerRuns runs,
            Environment environment,
            @Value("${eldercare.jobs.procurement-reminder.on-startup:${eldercare.scheduling.enabled:true}}")
                    boolean startup) {
        this.reminders = reminders;
        this.time = time;
        this.runs = runs;
        this.environment = environment;
        this.startup = startup;
    }

    @Scheduled(cron = "${eldercare.jobs.procurement-reminder.cron:0 0 7 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = NAME)
    public void scheduled() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = NAME)
    public void atStartup() {
        if (startup && !environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void runNow() {
        String run = runs.start(name());
        int count = 0;
        try {
            count = reminders.remind(time.now());
            runs.finish(run, SchedulerRuns.Result.SUCCEEDED, count);
        } catch (RuntimeException error) {
            runs.finish(run, SchedulerRuns.Result.FAILED, count);
            throw error;
        }
    }
}
