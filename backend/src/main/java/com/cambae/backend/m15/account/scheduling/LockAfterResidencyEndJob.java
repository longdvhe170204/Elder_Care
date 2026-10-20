package com.cambae.backend.m15.account.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m15.account.internal.auth.service.ResidencyEndLocks;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job khóa tài khoản người thân sau kết thúc lưu trú lúc 01:30 (002 T070; FR-049; tasks ghi {@code KhoaSauKetThucLuuTru};
 * giờ chạy kỹ thuật, không phải CFG); profile test tắt lịch, test gọi {@link #runNow()}.
 */
@Component
public class LockAfterResidencyEndJob implements ScheduledJob {

    private final ResidencyEndLocks locks;

    public LockAfterResidencyEndJob(ResidencyEndLocks locks) {
        this.locks = locks;
    }

    @Scheduled(cron = "${eldercare.jobs.lock-after-residency-end.cron:0 30 1 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "LOCK_AFTER_RESIDENCY_END")
    public void scheduled() {
        locks.run();
    }

    @Override
    public String name() {
        return "LOCK_AFTER_RESIDENCY_END";
    }

    @Override
    public void runNow() {
        locks.run();
    }
}
