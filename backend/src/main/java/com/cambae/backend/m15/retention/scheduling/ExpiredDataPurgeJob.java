package com.cambae.backend.m15.retention.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m15.retention.internal.ExpiredDataPurge;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job loại bỏ dữ liệu hết hạn lưu giữ, chạy hằng tháng (000 T071, research R-11). Lịch chạy là thông số kỹ thuật, không
 * phải CFG (plan 000, Complexity Tracking). Người thực hiện trong nhật ký là Bộ lập lịch: {@code CurrentUser} ngoài
 * request HTTP do 002 cung cấp (plan 000, D2).
 */
@Component
public class ExpiredDataPurgeJob implements ScheduledJob {

    private final ExpiredDataPurge purge;

    public ExpiredDataPurgeJob(ExpiredDataPurge purge) {
        this.purge = purge;
    }

    /** 02:30 ngày 1 hằng tháng, ngoài khung giao ca. */
    @Scheduled(cron = "0 30 2 1 * *", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "PURGE_EXPIRED_DATA")
    public void scheduled() {
        purge.run();
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
