package com.eldercare.backend.administration.retention.scheduling;

import com.eldercare.backend.administration.retention.internal.service.ExpiredDataPurge;
import com.eldercare.backend.common.scheduling.ScheduledJob;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job loại bỏ dữ liệu hết hạn lưu giữ, chạy hằng tháng (000 T071, research R-11). Lịch chạy là thông số kỹ thuật, không
 * phải CFG (plan 000, Complexity Tracking). Người thực hiện trong nhật ký là Bộ lập lịch: {@code CurrentUser} ngoài
 * request HTTP do 002 cung cấp (plan 000, D2). Sau khi khôi phục bản sao lưu, quản trị mở ứng dụng với
 * {@code eldercare.jobs.purge-expired-data.run-once=true} để chạy ngay một đợt (backend/docs/operations.md mục 5; FR-051).
 */
@Component
public class ExpiredDataPurgeJob implements ScheduledJob {

    private final ExpiredDataPurge purge;
    private final boolean runOnce;

    public ExpiredDataPurgeJob(
            ExpiredDataPurge purge, @Value("${eldercare.jobs.purge-expired-data.run-once:false}") boolean runOnce) {
        this.purge = purge;
        this.runOnce = runOnce;
    }

    /** 02:30 ngày 1 hằng tháng, ngoài khung giao ca. */
    @Scheduled(cron = "0 30 2 1 * *", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "PURGE_EXPIRED_DATA")
    public void scheduled() {
        purge.run();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = "PURGE_EXPIRED_DATA")
    public void atStartup() {
        if (runOnce) {
            purge.run();
        }
    }

    /** Chạy một đợt, trả id lần chạy ({@code scheduler_run}). */
    public long run() {
        return purge.run();
    }

    @Override
    public String name() {
        return "PURGE_EXPIRED_DATA";
    }

    @Override
    public void runNow() {
        run();
    }
}
