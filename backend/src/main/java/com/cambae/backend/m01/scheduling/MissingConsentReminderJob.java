package com.cambae.backend.m01.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m01.internal.consent.service.MissingConsentReminderService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job nhắc thiếu bản đồng ý, mỗi giờ kiểm tra hạn nhắc (001 T058; FR-012). */
@Component
public class MissingConsentReminderJob implements ScheduledJob {

    private final MissingConsentReminderService reminders;

    public MissingConsentReminderJob(MissingConsentReminderService reminders) {
        this.reminders = reminders;
    }

    @Scheduled(cron = "${eldercare.jobs.missing-consent-reminder.cron:0 0 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = MissingConsentReminderService.JOB)
    public void scheduled() {
        reminders.run();
    }

    @Override
    public String name() {
        return MissingConsentReminderService.JOB;
    }

    @Override
    public void runNow() {
        reminders.run();
    }
}
