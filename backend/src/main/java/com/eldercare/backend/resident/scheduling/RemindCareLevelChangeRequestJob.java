package com.eldercare.backend.resident.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.resident.internal.assessment.service.CareLevelChangeDecisionService;
import com.eldercare.backend.resident.internal.assessment.service.CareLevelChangeRequestReminderService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job nhắc Hành chính lập yêu cầu đổi mức chăm sóc, mỗi 15 phút (001 T064; FR-036, CFG-M03-02); cùng lượt nhắc ngày xem lại
 * quyết định giữ mức cũ (FR-036b, Q-424).
 */
@Component
public class RemindCareLevelChangeRequestJob implements ScheduledJob {

    private final CareLevelChangeRequestReminderService service;
    private final CareLevelChangeDecisionService decisions;

    public RemindCareLevelChangeRequestJob(
            CareLevelChangeRequestReminderService service, CareLevelChangeDecisionService decisions) {
        this.service = service;
        this.decisions = decisions;
    }

    @Scheduled(
            cron = "${eldercare.jobs.remind-care-level-change-request.cron:0 */15 * * * *}",
            zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = CareLevelChangeRequestReminderService.JOB)
    public void scheduled() {
        service.run();
        decisions.remindReviews();
    }

    @Override
    public String name() {
        return CareLevelChangeRequestReminderService.JOB;
    }

    @Override
    public void runNow() {
        service.run();
        decisions.remindReviews();
    }
}
