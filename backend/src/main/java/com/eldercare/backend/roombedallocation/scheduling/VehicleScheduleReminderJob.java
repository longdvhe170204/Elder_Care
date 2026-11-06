package com.eldercare.backend.roombedallocation.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.internal.vehicle.service.VehicleScheduleReminders;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 019 T003 phần lịch xe (research W-09 {@code ViecTheoPhutM03LichXe}; FR-018): mỗi phút nhắc lịch Đang dùng quá giờ về;
 * chạy bù lúc khởi động; không tạo lịch xe (Q-306).
 */
@Component
public class VehicleScheduleReminderJob implements ScheduledJob {
    static final String NAME = "REMIND_VEHICLE_SCHEDULES";

    private final VehicleScheduleReminders reminders;
    private final BusinessTime time;
    private final SchedulerRuns runs;
    private final Environment environment;
    private final boolean startup;

    public VehicleScheduleReminderJob(
            VehicleScheduleReminders reminders,
            BusinessTime time,
            SchedulerRuns runs,
            Environment environment,
            @Value("${eldercare.jobs.vehicle-schedule-reminder.on-startup:${eldercare.scheduling.enabled:true}}")
                    boolean startup) {
        this.reminders = reminders;
        this.time = time;
        this.runs = runs;
        this.environment = environment;
        this.startup = startup;
    }

    @Scheduled(cron = "${eldercare.jobs.vehicle-schedule-reminder.cron:0 * * * * *}", zone = "Asia/Ho_Chi_Minh")
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
