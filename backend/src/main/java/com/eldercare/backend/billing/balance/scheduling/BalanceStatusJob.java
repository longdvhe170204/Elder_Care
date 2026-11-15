package com.eldercare.backend.billing.balance.scheduling;

import com.eldercare.backend.billing.balance.internal.status.service.AfterFinalStatusReminders;
import com.eldercare.backend.billing.balance.internal.status.service.DailyBalanceStatus;
import com.eldercare.backend.billing.balance.internal.status.service.DepositRefundReminders;
import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job hằng ngày 02:00 của phần số dư (017 T021; tên logic {@code ViecHangNgayM11SoDu}; research E-07): tính tình trạng,
 * báo, nhắc lại; nhắc hoàn cọc (T026, FR-015a); nhắc sau trạng thái cuối (T031, FR-024); chạy bù lúc khởi động. Không ghi giao dịch (Q-306, {@code M11ArchTest}).
 */
@Component
public class BalanceStatusJob implements ScheduledJob {

    static final String NAME = "COMPUTE_BALANCE_STATUS";

    private final DailyBalanceStatus daily;
    private final DepositRefundReminders depositRefunds;
    private final AfterFinalStatusReminders afterFinal;
    private final SchedulerRuns runs;
    private final Environment environment;
    private final boolean startup;

    public BalanceStatusJob(
            DailyBalanceStatus daily,
            DepositRefundReminders depositRefunds,
            AfterFinalStatusReminders afterFinal,
            SchedulerRuns runs,
            Environment environment,
            @Value("${eldercare.jobs.balance-status.on-startup:${eldercare.scheduling.enabled:true}}")
                    boolean startup) {
        this.daily = daily;
        this.depositRefunds = depositRefunds;
        this.afterFinal = afterFinal;
        this.runs = runs;
        this.environment = environment;
        this.startup = startup;
    }

    @Scheduled(cron = "${eldercare.jobs.balance-status.cron:0 0 2 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = NAME)
    public void scheduled() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = NAME)
    public void atStartup() {
        if (startup && !environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void runNow() {
        String run = runs.start(name());
        int count = 0;
        try {
            count = daily.run();
            count += depositRefunds.remind();
            count += afterFinal.remind();
            runs.finish(run, SchedulerRuns.Result.SUCCEEDED, count);
        } catch (RuntimeException error) {
            runs.finish(run, SchedulerRuns.Result.FAILED, count);
            throw error;
        }
    }
}
