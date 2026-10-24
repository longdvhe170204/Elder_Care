package com.cambae.backend.m01.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m01.internal.assessment.service.ReassessmentNoticeService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job báo đánh giá lại định kỳ, mỗi 15 phút (001 T064; FR-039, BR-M01-02). */
@Component
public class NotifyPeriodicReassessmentJob implements ScheduledJob {

    private final ReassessmentNoticeService service;

    public NotifyPeriodicReassessmentJob(ReassessmentNoticeService service) {
        this.service = service;
    }

    @Scheduled(cron = "${eldercare.jobs.notify-periodic-reassessment.cron:0 */15 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = ReassessmentNoticeService.PERIODIC_JOB)
    public void scheduled() {
        service.runPeriodic();
    }

    @Override
    public String name() {
        return ReassessmentNoticeService.PERIODIC_JOB;
    }

    @Override
    public void runNow() {
        service.runPeriodic();
    }
}
