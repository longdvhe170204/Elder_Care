package com.cambae.backend.m01.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m01.internal.assessment.service.ReassessmentNoticeService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job cảnh báo quá hạn đánh giá lại, mỗi 15 phút (001 T064; FR-041). */
@Component
public class AlertOverdueReassessmentJob implements ScheduledJob {

    private final ReassessmentNoticeService service;

    public AlertOverdueReassessmentJob(ReassessmentNoticeService service) {
        this.service = service;
    }

    @Scheduled(cron = "${eldercare.jobs.alert-overdue-reassessment.cron:0 */15 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = ReassessmentNoticeService.OVERDUE_JOB)
    public void scheduled() {
        service.runOverdue();
    }

    @Override
    public String name() {
        return ReassessmentNoticeService.OVERDUE_JOB;
    }

    @Override
    public void runNow() {
        service.runOverdue();
    }
}
