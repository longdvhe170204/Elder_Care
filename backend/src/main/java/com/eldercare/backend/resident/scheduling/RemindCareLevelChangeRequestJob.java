package com.eldercare.backend.resident.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.resident.internal.assessment.service.CareLevelChangeRequestReminderService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job nhắc Hành chính lập yêu cầu đổi mức chăm sóc, mỗi 15 phút (001 T064; FR-036, CFG-M03-02). */
@Component
public class RemindCareLevelChangeRequestJob implements ScheduledJob {

    private final CareLevelChangeRequestReminderService service;

    public RemindCareLevelChangeRequestJob(CareLevelChangeRequestReminderService service) {
        this.service = service;
    }

    @Scheduled(
            cron = "${eldercare.jobs.remind-care-level-change-request.cron:0 */15 * * * *}",
            zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = CareLevelChangeRequestReminderService.JOB)
    public void scheduled() {
        service.run();
    }

    @Override
    public String name() {
        return CareLevelChangeRequestReminderService.JOB;
    }

    @Override
    public void runNow() {
        service.run();
    }
}
