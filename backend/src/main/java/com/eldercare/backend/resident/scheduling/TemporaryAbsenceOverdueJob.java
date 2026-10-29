package com.eldercare.backend.resident.scheduling;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.resident.internal.status.service.TemporaryAbsenceOverdueAlert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job cảnh báo Tạm vắng quá hạn trở lại, mỗi 10 phút (001 T051; BR-M01-01, FR-050). */
@Component
public class TemporaryAbsenceOverdueJob implements ScheduledJob {

    private final TemporaryAbsenceOverdueAlert alert;

    public TemporaryAbsenceOverdueJob(TemporaryAbsenceOverdueAlert alert) {
        this.alert = alert;
    }

    @Scheduled(cron = "${eldercare.jobs.temporary-absence-overdue.cron:0 */10 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = TemporaryAbsenceOverdueAlert.JOB)
    public void scheduled() {
        alert.run();
    }

    @Override
    public String name() {
        return TemporaryAbsenceOverdueAlert.JOB;
    }

    @Override
    public void runNow() {
        alert.run();
    }
}
