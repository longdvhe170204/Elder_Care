package com.cambae.backend.m15.account.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m15.account.auth.internal.InactiveAccounts;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job chuyển tài khoản người thân Không hoạt động lúc 01:00 và 06:00 (002 T043; FR-010; giờ chạy theo tasks, kỹ thuật,
 * không phải CFG); profile test tắt lịch, test gọi {@link #runNow()}.
 */
@Component
public class MarkInactiveAccountsJob implements ScheduledJob {

    private final InactiveAccounts inactiveAccounts;

    public MarkInactiveAccountsJob(InactiveAccounts inactiveAccounts) {
        this.inactiveAccounts = inactiveAccounts;
    }

    @Scheduled(cron = "${eldercare.jobs.mark-inactive-accounts.cron:0 0 1,6 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "MARK_INACTIVE_ACCOUNTS")
    public void scheduled() {
        inactiveAccounts.run();
    }

    @Override
    public String name() {
        return "MARK_INACTIVE_ACCOUNTS";
    }

    @Override
    public void runNow() {
        inactiveAccounts.run();
    }
}
