package com.eldercare.backend.resident.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.resident.internal.consent.service.MissingConsentReminderService;
import com.eldercare.backend.resident.internal.wish.service.WishReminderService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job nhắc thiếu bản đồng ý và thiếu phiếu nguyện vọng cuối đời, mỗi giờ kiểm tra hạn nhắc (001 T058, T069; FR-012,
 * FR-023d), cùng nhắc xem lại nhận định khi tới ngày xem lại (FR-023i). Cả ba việc cùng một job nhắc theo chu kỳ của
 * research H-12 (NhacThieuDongYNguyenVong), nên không thêm job mới.
 */
@Component
public class MissingConsentReminderJob implements ScheduledJob {

    private final MissingConsentReminderService reminders;
    private final WishReminderService wishReminders;

    public MissingConsentReminderJob(MissingConsentReminderService reminders, WishReminderService wishReminders) {
        this.reminders = reminders;
        this.wishReminders = wishReminders;
    }

    @Scheduled(cron = "${eldercare.jobs.missing-consent-reminder.cron:0 0 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = MissingConsentReminderService.JOB)
    public void scheduled() {
        runNow();
    }

    @Override
    public String name() {
        return MissingConsentReminderService.JOB;
    }

    @Override
    public void runNow() {
        reminders.run();
        wishReminders.runMissing();
        wishReminders.runReviews();
    }
}
