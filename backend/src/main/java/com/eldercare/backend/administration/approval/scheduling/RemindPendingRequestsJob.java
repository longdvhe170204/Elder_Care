package com.eldercare.backend.administration.approval.scheduling;

import com.eldercare.backend.administration.approval.internal.service.PendingRequestReminder;
import com.eldercare.backend.common.scheduling.ScheduledJob;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job nhắc yêu cầu Chờ duyệt quá hạn, 15 phút một lần (000 T094, research R-10; chu kỳ kỹ thuật, không phải CFG);
 * profile test tắt lịch, test gọi {@link #run()}.
 */
@Component
public class RemindPendingRequestsJob implements ScheduledJob {

    private final PendingRequestReminder reminder;

    public RemindPendingRequestsJob(PendingRequestReminder reminder) {
        this.reminder = reminder;
    }

    @Scheduled(cron = "${eldercare.jobs.remind-pending-requests.cron:0 */15 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "REMIND_PENDING_REQUESTS")
    public void scheduled() {
        reminder.run();
    }

    /** Chạy một đợt, trả id lần chạy ({@code scheduler_run}). */
    public long run() {
        return reminder.run();
    }

    @Override
    public String name() {
        return "REMIND_PENDING_REQUESTS";
    }

    @Override
    public void runNow() {
        run();
    }
}
