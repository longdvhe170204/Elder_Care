package com.eldercare.backend.roombedallocation.scheduling;

import com.eldercare.backend.common.scheduling.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.internal.asset.service.AssetReminders;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 019 T003 phần tài sản (research W-09 {@code ViecHangNgayM03TaiSan} 06:00; BR-M03-17): nhắc hạn, gắn dấu quá hạn;
 * chạy bù lúc khởi động; không tạo tài sản (Q-306, {@code AssetArchTest}).
 */
@Component
public class AssetReminderJob implements ScheduledJob {
    static final String NAME = "REMIND_ASSET_DUE_DATES";

    private final AssetReminders reminders;
    private final BusinessTime time;
    private final SchedulerRuns runs;
    private final Environment environment;
    private final boolean startup;

    public AssetReminderJob(
            AssetReminders reminders,
            BusinessTime time,
            SchedulerRuns runs,
            Environment environment,
            @Value("${eldercare.jobs.asset-reminder.on-startup:${eldercare.scheduling.enabled:true}}")
                    boolean startup) {
        this.reminders = reminders;
        this.time = time;
        this.runs = runs;
        this.environment = environment;
        this.startup = startup;
    }

    @Scheduled(cron = "${eldercare.jobs.asset-reminder.cron:0 0 6 * * *}", zone = "Asia/Ho_Chi_Minh")
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
